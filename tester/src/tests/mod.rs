use crate::registry::{registry, Registry};

pub mod example;

pub fn all_tests() -> Registry {
    // Add any additional tests here.
    registry![example::test_buy_tickets,]
}
