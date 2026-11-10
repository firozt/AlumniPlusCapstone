CREATE TABLE order_activity ( 
  event_id    VARCHAR2(36)  PRIMARY KEY, 
  order_id    NUMBER        NOT NULL, 
  event_type  VARCHAR2(12)  NOT NULL 
              CHECK (event_type IN ('SUBMITTED','ACCEPTED','CANCELLED')), 
  event_time  TIMESTAMP     NOT NULL, 
  actor       VARCHAR2(50)  NOT NULL 
); 
  

CREATE INDEX idx_activity_order ON order_activity (order_id); 
