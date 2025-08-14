-- Create notify structure for Transaction Log Tailing --

CREATE OR REPLACE FUNCTION orders.order_outbox_notify()
    RETURNS TRIGGER AS
$$
BEGIN
    -- 'order_outbox_changes' isimli kanala yeni satırın JSON halini gönderiyoruz
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
