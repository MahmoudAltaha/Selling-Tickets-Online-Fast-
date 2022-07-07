use eyre::Result;

use crate::{api::Reservation, context::TestCtx};

pub async fn test_buy_tickets(ctx: &TestCtx) -> Result<()> {
    const TICKETS: u64 = 1_000;

    ctx.launcher()
        .with_tickets(TICKETS)
        .with_balancer_threads(64)
        .launch()
        .await?;

    ctx.api.post_num_servers(1).await?;

    let mut session = ctx.api.create_user_session(None);

    for _ in 0..TICKETS {
        match session.reserve_ticket().await?.result? {
            Reservation::SoldOut => ctx.fail("Tickets should not be sold out!")?,
            Reservation::Reserved(ticket) => {
                ctx.check_eq(
                    session.buy_ticket(ticket).await?.result?,
                    ticket,
                    "Ticket id does not match!",
                )?;
            }
        }
    }

    Ok(())
}
