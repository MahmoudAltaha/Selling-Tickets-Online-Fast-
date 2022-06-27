use std::{error::Error, str::FromStr, time::Duration};

use eyre::Result;
use reqwest::{header::HeaderValue, Client, RequestBuilder, Response, StatusCode, Url};
use thiserror::Error;
use uuid::Uuid;

#[derive(Clone)]
pub struct Api {
    client: Client,
    base_url: Url,
}

#[derive(Debug, Error)]
#[error("Error 400: {0}")]
pub struct ApiError(String);

pub type ApiResult<T> = std::result::Result<T, ApiError>;

pub struct ApiResponse<T> {
    pub server_id: Option<Uuid>,
    pub customer_id: Option<Uuid>,
    pub result: ApiResult<T>,
}

impl<T> ApiResponse<T> {
    pub fn map_response<R, F: FnOnce(T) -> Result<R>>(self, func: F) -> Result<ApiResponse<R>> {
        let result = match self.result.map(func) {
            Ok(result) => Ok(result?),
            Err(err) => Err(err),
        };
        Ok(ApiResponse {
            server_id: self.server_id,
            customer_id: self.customer_id,
            result,
        })
    }
}

fn uuid_from_header(value: &HeaderValue) -> Result<Uuid> {
    Ok(Uuid::from_str(value.to_str()?)?)
}

impl Api {
    pub fn new(base_url: Url) -> Self {
        Self {
            client: Client::builder()
                .timeout(Duration::from_millis(1000))
                .build()
                .unwrap(),
            base_url,
        }
    }

    fn join_url(&self, path: &str) -> Url {
        self.base_url.join(path).unwrap()
    }

    fn prepare_request(
        &self,
        options: &RequestOptions,
        mut builder: RequestBuilder,
    ) -> RequestBuilder {
        if let Some(server_id) = options.server_id {
            builder = builder.header("X-Server-Id", server_id.to_string());
        }
        if let Some(customer_id) = options.customer_id {
            builder = builder.header("X-Customer-Id", customer_id.to_string());
        }
        builder
    }

    async fn process_response<R: 'static + FromStr>(
        &self,
        response: Response,
    ) -> Result<ApiResponse<R>>
    where
        <R as FromStr>::Err: Into<eyre::Report> + Send + Sync,
    {
        let headers = response.headers();
        let server_id = headers
            .get("X-Server-Id")
            .map(uuid_from_header)
            .transpose()?;
        let customer_id = headers
            .get("X-Customer-Id")
            .map(uuid_from_header)
            .transpose()?;
        let status = response.status();
        match status {
            StatusCode::OK => Ok(ApiResponse {
                server_id,
                customer_id,
                result: Ok(response.text().await?.parse().map_err(Into::into)?),
            }),
            StatusCode::BAD_REQUEST => Ok(ApiResponse {
                server_id,
                customer_id,
                result: Err(ApiError(response.text().await?)),
            }),
            _ => Err(eyre::eyre!("Server returned invalid status {status}.")),
        }
    }

    async fn get<R: 'static + FromStr>(
        &self,
        path: &str,
        options: &RequestOptions,
    ) -> Result<ApiResponse<R>>
    where
        <R as FromStr>::Err: Error + Send + Sync,
    {
        let response = self
            .prepare_request(options, self.client.get(self.join_url(path)))
            .send()
            .await?;
        self.process_response(response).await
    }

    async fn post<T: ToString + ?Sized, R: 'static + FromStr>(
        &self,
        path: &str,
        value: &T,
        options: &RequestOptions,
    ) -> Result<ApiResponse<R>>
    where
        <R as FromStr>::Err: Into<eyre::Report> + Send + Sync,
    {
        let response = self
            .prepare_request(options, self.client.post(self.join_url(path)))
            .body(value.to_string())
            .send()
            .await?;
        self.process_response(response).await
    }

    pub async fn get_num_servers(&self) -> Result<ApiResponse<usize>> {
        self.get("/api/admin/num_servers", &Default::default())
            .await
    }

    pub async fn post_num_servers(&self, number: usize) -> Result<ApiResponse<usize>> {
        self.post("/api/admin/num_servers", &number, &Default::default())
            .await
    }

    pub async fn get_servers(&self) -> Result<ApiResponse<Vec<Uuid>>> {
        self.get::<String>("/api/admin/get_servers", &Default::default())
            .await?
            .map_response(|response| {
                Ok(response
                    .split_ascii_whitespace()
                    .map(|server_id| Uuid::from_str(server_id))
                    .collect::<Result<Vec<_>, _>>()?)
            })
    }

    pub fn create_user_session(&self, server_id: Option<Uuid>) -> UserSession<'_> {
        UserSession {
            api: self,
            customer_id: Uuid::new_v4(),
            server_id,
            state: SessionState::None,
        }
    }
}

pub enum SessionState {
    None,
    Reserved(u64),
}

pub struct UserSession<'a> {
    pub api: &'a Api,
    pub customer_id: Uuid,
    pub server_id: Option<Uuid>,
    pub state: SessionState,
}

pub enum Reservation {
    SoldOut,
    Reserved(u64),
}

impl Reservation {
    pub fn reserved(&self) -> Result<u64> {
        match self {
            Reservation::SoldOut => Err(eyre::eyre!(
                "Reservation failed when it shall have succeeded."
            )),
            Reservation::Reserved(ticket_id) => Ok(*ticket_id),
        }
    }
}

impl FromStr for Reservation {
    type Err = eyre::Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.trim() {
            "SOLD OUT" => Ok(Self::SoldOut),
            s => Ok(Self::Reserved(s.parse()?)),
        }
    }
}

impl<'a> UserSession<'a> {
    fn request_options(&self) -> RequestOptions {
        RequestOptions {
            server_id: self.server_id.clone(),
            customer_id: Some(self.customer_id),
        }
    }

    fn process_response<T>(&mut self, response: ApiResponse<T>) -> ApiResponse<T> {
        self.server_id = response.server_id.clone();
        response
    }

    pub async fn get_available_tickets(&mut self) -> Result<ApiResponse<u64>> {
        Ok(self.process_response(
            self.api
                .get("/api/num_available_tickets", &self.request_options())
                .await?,
        ))
    }

    pub async fn reserve_ticket(&mut self) -> Result<ApiResponse<Reservation>> {
        let response: ApiResponse<Reservation> = self.process_response(
            self.api
                .post("/api/reserve_ticket", "", &self.request_options())
                .await?,
        );
        if let Ok(reservation) = &response.result {
            match reservation {
                Reservation::SoldOut => {
                    self.state = SessionState::None;
                }
                Reservation::Reserved(ticket_id) => {
                    self.state = SessionState::Reserved(*ticket_id);
                }
            }
        }
        Ok(response)
    }

    pub async fn abort_purchase(&mut self, ticket_id: u64) -> Result<ApiResponse<u64>> {
        Ok(self.process_response(
            self.api
                .post("/api/abort_purchase", &ticket_id, &self.request_options())
                .await?,
        ))
    }

    pub async fn buy_ticket(&mut self, ticket_id: u64) -> Result<ApiResponse<u64>> {
        Ok(self.process_response(
            self.api
                .post("/api/buy_ticket", &ticket_id, &self.request_options())
                .await?,
        ))
    }
}

#[derive(Default)]
pub struct RequestOptions {
    pub server_id: Option<Uuid>,
    pub customer_id: Option<Uuid>,
}
