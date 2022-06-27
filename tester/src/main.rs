use std::path::PathBuf;

use api::Api;
use clap::Parser;
use color_eyre::Help;
use logger::LoggerRef;
use reqwest::Url;
use tests::all_tests;
use tracing::info;

pub mod api;
pub mod context;
pub mod logger;
pub mod registry;
pub mod rocket;
pub mod tests;

#[derive(Parser, Debug)]
#[clap(about)]
pub struct Arguments {
    /// The `.jar` file.
    pub jar: PathBuf,
    /// Whether to use the bonus implementation.
    pub bonus: Option<bool>,
}

#[tokio::main]
async fn main() -> eyre::Result<()> {
    color_eyre::install()?;
    tracing_subscriber::fmt()
        .with_writer(std::io::stderr)
        .init();

    let args = Arguments::parse();

    let jar = tokio::fs::canonicalize(&args.jar)
        .await
        .with_suggestion(|| "Check whether the file exists.")?;

    let logger = LoggerRef::new();

    let api = Api::new(Url::parse("http://127.0.0.1:8585/")?);

    for test_case in all_tests().iter() {
        info!("⏳ Starting test {:?}.", test_case.path());
        let result = test_case
            .run(
                logger.clone(),
                jar.to_owned(),
                api.clone(),
                args.bonus.unwrap_or(false),
            )
            .await;
        match result {
            Ok(_) => {
                info!("✅ Test {:?} passed.", test_case.path());
            }
            Err(error) => {
                info!("❌ Test {:?} failed.", test_case.path());
                eprintln!("{:?}", error);
            }
        }
    }

    Ok(())
}
