use std::path::PathBuf;
use std::{fmt::Debug, path::Path};

use eyre::Result;
use parking_lot::Mutex;

use crate::logger::LoggerRef;
use crate::{
    api::Api,
    rocket::{Launcher, RocketRef},
};

pub struct TestCtx {
    pub jar: PathBuf,
    pub api: Api,
    pub errors: Mutex<Vec<String>>,
    pub logger: LoggerRef,
    pub bonus: bool,
    pub rockets: Mutex<Vec<RocketRef>>,
}

impl TestCtx {
    pub fn jar(&self) -> &Path {
        &self.jar
    }

    pub fn launcher(&self) -> Launcher {
        Launcher::new(self)
    }

    pub fn register_rocket(&self, rocket: RocketRef) {
        self.rockets.lock().push(rocket);
    }

    pub fn fail<S: Into<String>>(&self, message: S) -> Result<()> {
        let message = message.into();
        let error = Err(eyre::eyre!("Error: {}", message));
        self.errors.lock().push(message.into());
        error
    }

    pub fn check<S: Into<String>>(&self, passed: bool, message: S) -> Result<()> {
        let message = message.into();
        if passed {
            Ok(())
        } else {
            self.fail(message)
        }
    }

    pub fn check_eq<Y: Debug, X: Debug + PartialEq<Y>, S: Into<String>>(
        &self,
        got: X,
        expected: Y,
        message: S,
    ) -> Result<()> {
        self.check(
            got == expected,
            format!(
                "Expected {:?} but got {:?}: {}",
                expected,
                got,
                message.into()
            ),
        )
    }
}
