CREATE TABLE public.account (
    id character varying(255) NOT NULL,
    balance double precision NOT NULL,
    created_date timestamp without time zone,
    interest_rate real,
    type character varying(255),
    updated_date timestamp without time zone,
    member_id bigint
);


ALTER TABLE public.account OWNER TO smartsapp;

--
-- TOC entry 216 (class 1259 OID 198132)
-- Name: account_guarantors; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.account_guarantors (
    account_id character varying(255) NOT NULL,
    guarantors_id bigint NOT NULL
);


ALTER TABLE public.account_guarantors OWNER TO smartsapp;

--
-- TOC entry 217 (class 1259 OID 198135)
-- Name: account_transactions; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.account_transactions (
    account_id character varying(255) NOT NULL,
    transactions_id bigint NOT NULL
);


ALTER TABLE public.account_transactions OWNER TO smartsapp;

--
-- TOC entry 219 (class 1259 OID 198139)
-- Name: guarantor; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.guarantor (
    id bigint NOT NULL,
    amount real,
    created_date timestamp without time zone,
    fund_released boolean NOT NULL,
    updated_date timestamp without time zone,
    member_id bigint
);


ALTER TABLE public.guarantor OWNER TO smartsapp;

--
-- TOC entry 218 (class 1259 OID 198138)
-- Name: guarantor_id_seq; Type: SEQUENCE; Schema: public; Owner: smartsapp
--

CREATE SEQUENCE public.guarantor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.guarantor_id_seq OWNER TO smartsapp;

--
-- TOC entry 3402 (class 0 OID 0)
-- Dependencies: 218
-- Name: guarantor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: smartsapp
--

ALTER SEQUENCE public.guarantor_id_seq OWNED BY public.guarantor.id;


--
-- TOC entry 220 (class 1259 OID 198145)
-- Name: member; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.member (
    id bigint NOT NULL,
    address character varying(255),
    city character varying(255),
    created_date timestamp without time zone,
    email character varying(255),
    first_of_kin_email character varying(255),
    first_of_kin_name character varying(255),
    first_of_kin_phone character varying(255),
    gender character varying(255),
    image character varying(255),
    name character varying(255),
    phone character varying(255),
    second_of_kin_email character varying(255),
    second_of_kin_name character varying(255),
    second_of_kin_phone character varying(255),
    total_balance double precision,
    transaction_count bigint NOT NULL,
    updated_date timestamp without time zone
);


ALTER TABLE public.member OWNER TO smartsapp;

--
-- TOC entry 221 (class 1259 OID 198152)
-- Name: member_accounts; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.member_accounts (
    member_id bigint NOT NULL,
    accounts_id character varying(255) NOT NULL
);


ALTER TABLE public.member_accounts OWNER TO smartsapp;

--
-- TOC entry 223 (class 1259 OID 198156)
-- Name: transaction; Type: TABLE; Schema: public; Owner: smartsapp
--

CREATE TABLE public.transaction (
    id bigint NOT NULL,
    amount real,
    comment character varying(255),
    created_date timestamp without time zone,
    type character varying(255),
    updated_date timestamp without time zone,
    account_id character varying(255),
    created_by_id bigint
);


ALTER TABLE public.transaction OWNER TO smartsapp;

--
-- TOC entry 222 (class 1259 OID 198155)
-- Name: transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: smartsapp
--

CREATE SEQUENCE public.transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transaction_id_seq OWNER TO smartsapp;

--
-- TOC entry 3403 (class 0 OID 0)
-- Dependencies: 222
-- Name: transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: smartsapp
--

ALTER SEQUENCE public.transaction_id_seq OWNED BY public.transaction.id;


--
-- TOC entry 3228 (class 2604 OID 198142)
-- Name: guarantor id; Type: DEFAULT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.guarantor ALTER COLUMN id SET DEFAULT nextval('public.guarantor_id_seq'::regclass);


--
-- TOC entry 3229 (class 2604 OID 198159)
-- Name: transaction id; Type: DEFAULT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.transaction ALTER COLUMN id SET DEFAULT nextval('public.transaction_id_seq'::regclass);


--
-- TOC entry 3231 (class 2606 OID 198131)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- TOC entry 3237 (class 2606 OID 198144)
-- Name: guarantor guarantor_pkey; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.guarantor
    ADD CONSTRAINT guarantor_pkey PRIMARY KEY (id);


--
-- TOC entry 3239 (class 2606 OID 198151)
-- Name: member member_pkey; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);


--
-- TOC entry 3243 (class 2606 OID 198163)
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- TOC entry 3233 (class 2606 OID 198165)
-- Name: account_guarantors uk_4lyf4ldgir36bd3n0eoeq0no1; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_guarantors
    ADD CONSTRAINT uk_4lyf4ldgir36bd3n0eoeq0no1 UNIQUE (guarantors_id);


--
-- TOC entry 3235 (class 2606 OID 198167)
-- Name: account_transactions uk_hwp3451tqfjdhmlqw8ruc7y0k; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT uk_hwp3451tqfjdhmlqw8ruc7y0k UNIQUE (transactions_id);


--
-- TOC entry 3241 (class 2606 OID 198169)
-- Name: member_accounts uk_onwnmepkrpue953653owcvksp; Type: CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.member_accounts
    ADD CONSTRAINT uk_onwnmepkrpue953653owcvksp UNIQUE (accounts_id);


--
-- TOC entry 3245 (class 2606 OID 198180)
-- Name: account_guarantors fk12ron1vgb2iuoesmrmhq3e4xu; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_guarantors
    ADD CONSTRAINT fk12ron1vgb2iuoesmrmhq3e4xu FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- TOC entry 3249 (class 2606 OID 198195)
-- Name: guarantor fk2ug5dduqdxasgiixynb3wya4o; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.guarantor
    ADD CONSTRAINT fk2ug5dduqdxasgiixynb3wya4o FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- TOC entry 3246 (class 2606 OID 198175)
-- Name: account_guarantors fk5t947821e4cur2ud1d3gbv2ly; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_guarantors
    ADD CONSTRAINT fk5t947821e4cur2ud1d3gbv2ly FOREIGN KEY (guarantors_id) REFERENCES public.guarantor(id);


--
-- TOC entry 3252 (class 2606 OID 198210)
-- Name: transaction fk6g20fcr3bhr6bihgy24rq1r1b; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT fk6g20fcr3bhr6bihgy24rq1r1b FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- TOC entry 3250 (class 2606 OID 198200)
-- Name: member_accounts fk8qwatmhnpotgvkk314tg9fak9; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.member_accounts
    ADD CONSTRAINT fk8qwatmhnpotgvkk314tg9fak9 FOREIGN KEY (accounts_id) REFERENCES public.account(id);


--
-- TOC entry 3247 (class 2606 OID 198185)
-- Name: account_transactions fkbo9ocnxyf4qmchbuu7nwgfw90; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT fkbo9ocnxyf4qmchbuu7nwgfw90 FOREIGN KEY (transactions_id) REFERENCES public.transaction(id);


--
-- TOC entry 3248 (class 2606 OID 198190)
-- Name: account_transactions fkbrn3bd376gr5d364db3tn3rx9; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account_transactions
    ADD CONSTRAINT fkbrn3bd376gr5d364db3tn3rx9 FOREIGN KEY (account_id) REFERENCES public.account(id);


--
-- TOC entry 3253 (class 2606 OID 198215)
-- Name: transaction fkc35oldk6267xoha9xglpivyjp; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT fkc35oldk6267xoha9xglpivyjp FOREIGN KEY (created_by_id) REFERENCES public.member(id);


--
-- TOC entry 3251 (class 2606 OID 198205)
-- Name: member_accounts fko1htkfaq2fuavxsiy3kph6b0k; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.member_accounts
    ADD CONSTRAINT fko1htkfaq2fuavxsiy3kph6b0k FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- TOC entry 3244 (class 2606 OID 198170)
-- Name: account fkr5j0huynd7nsv1s7e9vb8qvwo; Type: FK CONSTRAINT; Schema: public; Owner: smartsapp
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT fkr5j0huynd7nsv1s7e9vb8qvwo FOREIGN KEY (member_id) REFERENCES public.member(id);


-- Completed on 2025-07-11 21:09:00 GMT

--
-- PostgreSQL database dump complete
--

