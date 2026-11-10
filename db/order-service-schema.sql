CREATE TABLE instruments ( 
  instrument_id  NUMBER        PRIMARY KEY, 
  symbol         VARCHAR2(10)  NOT NULL UNIQUE, 
  name           VARCHAR2(100) NOT NULL, 
  asset_class    VARCHAR2(20)  NOT NULL, 
  currency       CHAR(3)       NOT NULL, 
  active         CHAR(1)       DEFAULT 'Y' NOT NULL 
                 CHECK (active IN ('Y','N')) 
); 
  

CREATE SEQUENCE order_seq START WITH 1000 INCREMENT BY 1 NOCACHE; 
  

CREATE TABLE orders ( 
  order_id        NUMBER        PRIMARY KEY, 
  owner_username  VARCHAR2(50)  NOT NULL, 
  instrument_id   NUMBER        NOT NULL 
                  REFERENCES instruments (instrument_id), 
  side            VARCHAR2(4)   NOT NULL CHECK (side IN ('BUY','SELL')), 
  quantity        NUMBER(12)    NOT NULL CHECK (quantity > 0), 
  limit_price     NUMBER(12,4)  NOT NULL CHECK (limit_price > 0), 
  status          VARCHAR2(10)  DEFAULT 'SUBMITTED' NOT NULL 
                  CHECK (status IN ('SUBMITTED','ACCEPTED','CANCELLED')), 
  created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL, 
  updated_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL 
); 

  
CREATE INDEX idx_orders_owner  ON orders (owner_username); 
CREATE INDEX idx_orders_status ON orders (status); 

  

INSERT INTO instruments VALUES (1,'ABC','Alpha Bank Corp','Equity','USD','Y'); 
INSERT INTO instruments VALUES (2,'GLD','Global Dynamics Ltd','Equity','USD','Y'); 
INSERT INTO instruments VALUES (3,'EUB10','Euro Government Bond 10Y','Bond','EUR','Y'); 
INSERT INTO instruments VALUES (4,'TCH','TechWave Inc','Equity','USD','Y'); 
INSERT INTO instruments VALUES (5,'GBP-ETF','UK Index Tracker','ETF','GBP','Y'); 

COMMIT; 
