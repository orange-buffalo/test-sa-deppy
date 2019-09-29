create sequence hibernate_sequence start with 1 increment by 1
create table category (id bigint not null, version integer not null, description varchar(1024), expense boolean not null, income boolean not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table customer (id bigint not null, version integer not null, name varchar(255) not null, workspace_id bigint not null, primary key (id))
create table document (id bigint not null, version integer not null, name varchar(255) not null, notes varchar(1024), size_in_bytes bigint, storage_provider_id varchar(255) not null, storage_provider_location varchar(2048), time_uploaded timestamp not null, workspace_id bigint not null, primary key (id))
create table expense (id bigint not null, version integer not null, actual_amount_in_default_currency bigint not null, amount_in_default_currency bigint not null, currency varchar(3) not null, date_paid date not null, notes varchar(1024), original_amount bigint not null, percent_on_business integer not null, reported_amount_in_default_currency bigint not null, tax_amount bigint, tax_rate_in_bps integer, time_recorded timestamp not null, title varchar(255) not null, category_id bigint, tax_id bigint, workspace_id bigint not null, primary key (id))
create table expense_attachments (expense_id bigint not null, document_id bigint not null, primary key (expense_id, document_id))
create table google_drive_storage_integration (id bigint not null, version integer not null, folder_id varchar(255), user_id bigint not null, primary key (id))
create table income (id bigint not null, version integer not null, amount_in_default_currency bigint not null, currency varchar(3) not null, date_received date not null, notes varchar(1024), original_amount bigint not null, reported_amount_in_default_currency bigint not null, tax_amount bigint, tax_rate_in_bps integer, time_recorded timestamp not null, title varchar(255) not null, category_id bigint, tax_id bigint, workspace_id bigint not null, primary key (id))
create table income_attachments (income_id bigint not null, document_id bigint not null, primary key (income_id, document_id))
create table invoice (id bigint not null, version integer not null, amount bigint not null, currency varchar(3) not null, date_cancelled date, date_issued date not null, date_paid date, date_sent date, due_date date not null, notes varchar(1024), time_recorded timestamp not null, title varchar(255) not null, customer_id bigint not null, income_id bigint, tax_id bigint, primary key (id))
create table invoice_attachments (invoice_id bigint not null, document_id bigint not null, primary key (invoice_id, document_id))
create table persistent_oauth2_authorization_request (id bigint not null, version integer not null, client_registration_id varchar(255) not null, create_when timestamp not null, state varchar(512) not null, owner_id bigint not null, primary key (id))
create table persistent_oauth2_authorized_client (id bigint not null, version integer not null, access_token varchar(255) not null, access_token_expires_at timestamp, access_token_issued_at timestamp, client_registration_id varchar(255) not null, refresh_token varchar(255), refresh_token_issued_at timestamp, user_name varchar(255) not null, primary key (id))
create table persistent_oauth2_authorized_client_access_token_scopes (client_id bigint not null, access_token_scopes varchar(255))
create table platform_user (id bigint not null, version integer not null, documents_storage varchar(255), is_admin boolean not null, password_hash varchar(255) not null, user_name varchar(255) not null, primary key (id))
create table refresh_token (id bigint not null, version integer not null, expiration_time timestamp not null, token varchar(2048) not null, user_id bigint not null, primary key (id))
create table saved_workspace_access_token (id bigint not null, version integer not null, owner_id bigint not null, workspace_access_token_id bigint not null, primary key (id))
create table tax (id bigint not null, version integer not null, description varchar(255), rate_in_bps integer not null, title varchar(255) not null, workspace_id bigint not null, primary key (id))
create table tax_payment_attachments (tax_payment_id bigint not null, document_id bigint not null, primary key (tax_payment_id, document_id))
create table tax_payment (id bigint not null, version integer not null, amount bigint not null, date_paid date not null, notes varchar(1024), reporting_date date not null, time_recorded timestamp not null, title varchar(255) not null, workspace_id bigint not null, primary key (id))
create table workspace (id bigint not null, version integer not null, default_currency varchar(255) not null, multi_currency_enabled boolean not null, name varchar(255) not null, tax_enabled boolean not null, owner_id bigint not null, primary key (id))
create table workspace_access_token (id bigint not null, version integer not null, revoked boolean not null, time_created timestamp not null, token varchar(255) not null, valid_till timestamp not null, workspace_id bigint not null, primary key (id))
alter table workspace_access_token add constraint workspace_access_token_token_uq unique (token)
alter table category add constraint category_workspace_fk foreign key (workspace_id) references workspace
alter table customer add constraint customer_workspace_fk foreign key (workspace_id) references workspace
alter table document add constraint document_workspace_fk foreign key (workspace_id) references workspace
alter table expense add constraint expense_category_fk foreign key (category_id) references category
alter table expense add constraint expense_tax_fk foreign key (tax_id) references tax
alter table expense add constraint expense_workspace_fk foreign key (workspace_id) references workspace
alter table expense_attachments add constraint expense_attachments_document_fk foreign key (document_id) references document
alter table expense_attachments add constraint expense_attachments_expense_fk foreign key (expense_id) references expense
alter table google_drive_storage_integration add constraint gdrive_storage_integration_user_fk foreign key (user_id) references platform_user
alter table income add constraint income_category_fk foreign key (category_id) references category
alter table income add constraint income_tax_fk foreign key (tax_id) references tax
alter table income add constraint income_workspace_fk foreign key (workspace_id) references workspace
alter table income_attachments add constraint income_attachments_document_fk foreign key (document_id) references document
alter table income_attachments add constraint income_attachments_income_fk foreign key (income_id) references income
alter table invoice add constraint invoice_customer_fk foreign key (customer_id) references customer
alter table invoice add constraint invoice_income_fk foreign key (income_id) references income
alter table invoice add constraint invoice_tax_fk foreign key (tax_id) references tax
alter table invoice_attachments add constraint invoice_attachments_document_fk foreign key (document_id) references document
alter table invoice_attachments add constraint invoice_attachments_invoice_fk foreign key (invoice_id) references invoice
alter table persistent_oauth2_authorization_request add constraint persistent_oauth2_authorization_request_owner_fk foreign key (owner_id) references platform_user
alter table persistent_oauth2_authorized_client_access_token_scopes add constraint pauth2ac_access_token_scopes_scopes_client_fk foreign key (client_id) references persistent_oauth2_authorized_client
alter table refresh_token add constraint refresh_token_user_fk foreign key (user_id) references platform_user
alter table saved_workspace_access_token add constraint saved_ws_access_token_owner_fk foreign key (owner_id) references platform_user
alter table saved_workspace_access_token add constraint saved_ws_access_token_ws_access_token_fk foreign key (workspace_access_token_id) references workspace_access_token
alter table tax add constraint tax_workspace_fk foreign key (workspace_id) references workspace
alter table tax_payment_attachments add constraint tax_payment_attachments_document_fk foreign key (document_id) references document
alter table tax_payment_attachments add constraint tax_payment_attachments_tax_payment_fk foreign key (tax_payment_id) references tax_payment
alter table tax_payment add constraint tax_payment_workspace_fk foreign key (workspace_id) references workspace
alter table workspace add constraint workspace_owner_fk foreign key (owner_id) references platform_user
alter table workspace_access_token add constraint workspace_access_token_workspace_fk foreign key (workspace_id) references workspace