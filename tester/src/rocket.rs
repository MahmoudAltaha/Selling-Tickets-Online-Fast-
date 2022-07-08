use std::{sync::Arc, time::Duration};

use eyre::Result;
use tokio::{
    process::{Child, Command},
    time::{sleep, timeout},
};
use tracing::warn;

use crate::context::TestCtx;

pub struct RocketShared {
    pub child: tokio::sync::Mutex<Child>,
}

#[derive(Clone)]
pub struct RocketRef(pub Arc<RocketShared>);

impl RocketRef {
    pub async fn kill(&self) {
        let mut child = self.0.child.lock().await;
        let kill = timeout(Duration::from_millis(1000), child.kill());
        if kill.await.is_err() {
            warn!("Rocket kill timeout has expired.");
        }
    }
}

pub struct Launcher<'cx> {
    ctx: &'cx TestCtx,
    cmd: Command,
}

impl<'cx> Launcher<'cx> {
    pub fn new(ctx: &'cx TestCtx) -> Self {
        let mut cmd = Command::new("java");
        cmd.arg("-ea").arg("-jar").arg(ctx.jar()).kill_on_drop(true);
        if ctx.bonus {
            cmd.arg("-bonus");
        }
        Self { ctx, cmd }
    }

    pub fn with_tickets(mut self, available: u64) -> Self {
        self.cmd.arg("-tickets").arg(available.to_string());
        self
    }

    pub fn with_balancer_threads(mut self, threads: u16) -> Self {
        self.cmd.arg("-balancer-threads").arg(threads.to_string());
        self
    }

    pub fn with_timeout(mut self, timeout: u16) -> Self {
        self.cmd.arg("-timeout").arg(timeout.to_string());
        self
    }

    pub async fn launch(mut self) -> Result<RocketRef> {
        let mut child = self.cmd.spawn()?;

        // Give the system some time to start.
        sleep(Duration::from_millis(200)).await;

        if let Ok(status) = child.wait().await {
            eprintln!("Java exited pre-maturely with status code {}.", status);
        }

        let rocket = RocketRef(Arc::new(RocketShared {
            child: tokio::sync::Mutex::new(child),
        }));

        self.ctx.register_rocket(rocket.clone());

        Ok(rocket)
    }
}
