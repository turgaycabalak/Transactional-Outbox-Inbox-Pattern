-- Create notify structure for Transaction Log Tailing --

CREATE OR REPLACE FUNCTION orders.order_outbox_notify()
    RETURNS TRIGGER AS
$$
BEGIN
    -- We send the JSON version of the new row to the channel named 'order_outbox_created_notify'
    PERFORM pg_notify('order_outbox_created_notify', row_to_json(NEW)::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;



DROP TRIGGER IF EXISTS order_outbox_insert_trigger ON orders.order_outbox;

CREATE TRIGGER order_outbox_insert_trigger
    AFTER INSERT
    ON orders.order_outbox
    FOR EACH ROW
EXECUTE FUNCTION orders.order_outbox_notify();
--======================================================================================================

CREATE OR REPLACE FUNCTION analytics.order_inbox_notify()
    RETURNS TRIGGER AS
$$
BEGIN
    -- We send the JSON version of the new row to the channel named 'order_inbox_created_notify'
    PERFORM pg_notify('order_inbox_created_notify', row_to_json(NEW)::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;



DROP TRIGGER IF EXISTS order_inbox_insert_trigger ON analytics.order_inbox;

CREATE TRIGGER order_inbox_insert_trigger
    AFTER INSERT
    ON analytics.order_inbox
    FOR EACH ROW
EXECUTE FUNCTION analytics.order_inbox_notify();