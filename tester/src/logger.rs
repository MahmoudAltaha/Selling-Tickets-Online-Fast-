use parking_lot::Mutex;
use std::{fmt::Debug, sync::Arc};

#[derive(Default, Debug, Clone)]
pub struct LoggerRef {
    log: Arc<Mutex<String>>,
}

impl LoggerRef {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn log_str(&self, string: &str) {
        let mut log = self.log.lock();
        eprintln!("{}", string);
        log.push_str(string);
    }

    pub fn log_err(&self, error: &dyn Debug) {
        self.log_str(&format!("{:?}", error));
    }

    pub fn log_result<T, E: Debug>(&self, result: Result<T, E>) -> Result<T, E> {
        match result {
            Ok(value) => Ok(value),
            Err(error) => {
                self.log_err(&error);
                Err(error)
            }
        }
    }

    pub fn to_string(&self) -> String {
        self.log.lock().clone()
    }
}
