use std::{path::PathBuf, sync::Arc};

use eyre::Result;
use futures::future::BoxFuture;

use crate::{api::Api, context::TestCtx, logger::LoggerRef};

#[derive(Clone)]
pub struct TestCase {
    path: String,
    run: Arc<dyn Send + Sync + for<'cx> Fn(&'cx TestCtx) -> BoxFuture<'cx, Result<()>>>,
}

impl TestCase {
    pub fn new(
        path: String,
        run: Arc<dyn Send + Sync + for<'cx> Fn(&'cx TestCtx) -> BoxFuture<'cx, Result<()>>>,
    ) -> Self {
        Self { path, run }
    }

    pub fn path(&self) -> &str {
        self.path.as_str()
    }

    pub async fn run(&self, logger: LoggerRef, jar: PathBuf, api: Api, bonus: bool) -> Result<()> {
        let ctx = TestCtx {
            jar,
            api,
            bonus,
            logger: logger.clone(),
            errors: Default::default(),
            rockets: Default::default(),
        };

        let result = (self.run)(&ctx).await;

        let rockets = ctx.rockets.lock().drain(..).collect::<Vec<_>>();

        for rocket in rockets {
            rocket.kill().await;
        }

        let errors = ctx.errors.into_inner();
        for error in &errors {
            eprintln!("❌ {}", error)
        }

        result.and_then(|_| match errors.get(0) {
            Some(message) => Err(eyre::eyre!("{}", message)),
            None => Ok(()),
        })
    }
}

#[derive(Default)]
pub struct Registry {
    cases: Vec<TestCase>,
}

impl Registry {
    pub fn register(&mut self, case: TestCase) {
        self.cases.push(case);
    }

    pub fn iter(&self) -> impl Iterator<Item = &TestCase> {
        self.cases.iter()
    }
}

macro_rules! registry {
    ($($func:path),*$(,)?) => {{
        use std::sync::Arc;

        use $crate::registry::{Registry, TestCase};

        let mut registry = Registry::default();
        $(
            registry.register(TestCase::new(stringify!($func).to_owned(), Arc::new(|ctx| Box::pin($func(ctx)))));
        )*
        registry
    }};
}

pub(crate) use registry;
