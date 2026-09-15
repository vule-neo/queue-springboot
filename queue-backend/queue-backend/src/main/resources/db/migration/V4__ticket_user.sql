-- Ciji je ticket. NULLABLE: stari ticketi nemaju vlasnika, a i salter
-- moze izdati broj za nekoga bez naloga (papirni fallback).
-- Bez ovoga svako prijavljen moze otkazati tudji ticket.
ALTER TABLE ticket ADD COLUMN user_id BIGINT;

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES app_user (id);

-- "Moji ticketi" upit u buducnosti.
CREATE INDEX ix_ticket_user ON ticket (user_id);
