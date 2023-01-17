drop database coccu;
create database coccu;
\q
-------------------------
\c coccu
UPDATE "transaction" SET account_id='LOAN-200-1' WHERE account_id='LOAN-200-2' AND amount=100 AND created_date::date='2015-08-15' AND type='LOAN_REPAYMENT';
UPDATE "transaction" SET account_id='LOAN-406-1' WHERE account_id='LOAN-406-2' AND amount=300 AND created_date::date='2015-02-08' AND type='LOAN';
DELETE FROM "account"  WHERE id='LOAN-406-2';