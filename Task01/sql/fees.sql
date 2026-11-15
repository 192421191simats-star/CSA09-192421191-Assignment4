-- Task 1 redesign - MySQL table used by JdbcFeeRepository
CREATE TABLE IF NOT EXISTS fee_payments (
  receipt_no  VARCHAR(20)   PRIMARY KEY,
  reg_no      CHAR(9)       NOT NULL,
  amount      DECIMAL(10,2) NOT NULL CHECK (amount > 0),
  bank_ref    VARCHAR(30)   NOT NULL,
  paid_at     DATETIME      NOT NULL,
  INDEX idx_fee_reg (reg_no, paid_at)
);
