DROP TABLE IF EXISTS control_results CASCADE;
DROP TABLE IF EXISTS standards CASCADE;
DROP TABLE IF EXISTS controls CASCADE;
DROP TABLE IF EXISTS control_summaries CASCADE;
DROP TABLE IF EXISTS cadets CASCADE;
DROP TABLE IF EXISTS universities CASCADE;
DROP TABLE IF EXISTS teachers CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS trainings CASCADE;
DROP TABLE IF EXISTS articles CASCADE;
DROP TABLE IF EXISTS article_tags CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS approaches CASCADE;
DROP TABLE IF EXISTS exercise_catalogs CASCADE;
DROP TABLE IF EXISTS exercises_in_training CASCADE;
DROP TABLE IF EXISTS exercise_parameters CASCADE;
DROP TABLE IF EXISTS groups CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS group_teachers CASCADE;
DROP TABLE IF EXISTS inspectors CASCADE;
DROP TABLE IF EXISTS measurement_units CASCADE;
DROP TABLE IF EXISTS control_standards CASCADE;

-- UNIVERSITIES

CREATE TABLE universities (
    id      bigserial PRIMARY KEY,
    code    varchar(128) NOT NULL,
    mark    decimal(4,2),
    CONSTRAINT CK_universities_mark CHECK (mark BETWEEN 0 AND 10)
);

-- USERS

CREATE TABLE users (
    id               bigserial PRIMARY KEY,
    first_name       varchar(64) NOT NULL,
    last_name        varchar(64) NOT NULL,
    patronymic       varchar(64),
    sex              varchar(16) NOT NULL,
    university_id    bigint,
    mail             varchar(256) NOT NULL UNIQUE,
    phone_number     varchar(13) NOT NULL UNIQUE,
    login            varchar(64) NOT NULL UNIQUE,
    password         varchar(256) NOT NULL,
    CONSTRAINT FK_users_university_id FOREIGN KEY (university_id)
        REFERENCES universities(id)
        ON UPDATE CASCADE
        ON DELETE NO ACTION,
    CONSTRAINT CK_users_sex CHECK (sex IN ('М','Ж'))
);

-- GROUPS

CREATE TABLE groups (
    id                 bigserial PRIMARY KEY,
    number             varchar(32) NOT NULL,
    foundation_date    date NOT NULL DEFAULT CURRENT_DATE,
    university_id      bigint NOT NULL,
    CONSTRAINT FK_groups_university_id FOREIGN KEY (university_id)
        REFERENCES universities(id)
        ON UPDATE CASCADE
        ON DELETE NO ACTION,
    CONSTRAINT UQ_groups_number_university UNIQUE (number, university_id),
    CONSTRAINT CK_groups_foundation_date CHECK (foundation_date <= CURRENT_DATE)
);

-- CADETS

CREATE TABLE cadets (
    user_id          bigint PRIMARY KEY,
    group_id         bigint NOT NULL,
    date_of_birth    date NOT NULL,
    weight           decimal(6,3) NOT NULL DEFAULT 75.00,
    military_rank    varchar(64) NOT NULL,
    post             varchar(64) NOT NULL,
	course           smallint  NOT NULL DEFAULT 1,
    CONSTRAINT FK_cadets_user FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT FK_cadets_group FOREIGN KEY (group_id)
        REFERENCES groups(id)
        ON UPDATE CASCADE
        ON DELETE NO ACTION,
    CONSTRAINT CK_cadets_weight CHECK (weight BETWEEN 30 AND 180),
	CONSTRAINT CK_cadets_course CHECK (course BETWEEN 1 AND 5),
    CONSTRAINT CK_cadets_rank CHECK (military_rank IN
        ('рядовой','ефрейтор','младший сержант','сержант','старший сержант','старшина')),
    CONSTRAINT CK_cadets_post CHECK (post IN ('КО-1','КО-2','КО-3','КО-4','ЗКВ', 'Курсант'))
);

-- TEACHERS

CREATE TABLE teachers (
    user_id          bigint PRIMARY KEY,
    qualification    varchar(64),
    military_rank    varchar(64) NOT NULL,
    post             varchar(64),
    CONSTRAINT FK_teachers_user FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT CK_teachers_rank CHECK (military_rank IN
        ('младший лейтенант','лейтенант','старший лейтенант',
         'капитан','майор','подполковник','полковник'))
);

-- GROUP_TEACHERS

CREATE TABLE group_teachers (
    teacher_id bigint,
    group_id   bigint,
    PRIMARY KEY (teacher_id, group_id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

-- MESSAGES

CREATE TABLE messages (
    id           bigserial PRIMARY KEY,
    sender_id    bigint,
    recipient_id bigint,
    topic        varchar(256) NOT NULL,
    text         text NOT NULL,
    sent_at      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_reading   boolean NOT NULL DEFAULT false,
    FOREIGN KEY (sender_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    FOREIGN KEY (recipient_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_messages_sent_at CHECK (sent_at <= CURRENT_TIMESTAMP)
);

-- CONTROLS

CREATE TABLE controls (
    id         bigserial PRIMARY KEY,
    type       varchar(64) NOT NULL,
    group_id   bigint NOT NULL,
    date       date NOT NULL DEFAULT CURRENT_DATE,
    created_by bigint,
    FOREIGN KEY (group_id) REFERENCES groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES teachers(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT CK_controls_type CHECK (type IN
        ('Контрольное занятие','Зачет','Экзамен','Смотр-конкурс')),
    CONSTRAINT CK_controls_date CHECK (date <= CURRENT_DATE)
);

-- CONTROL_SUMMARIES

CREATE TABLE control_summaries (
    control_id          bigint,
    cadet_id            bigint,
    cadet_military_rank varchar(64),
    cadet_post          varchar(64),
    final_mark          smallint,
    PRIMARY KEY (control_id, cadet_id),
    FOREIGN KEY (control_id) REFERENCES controls(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (cadet_id) REFERENCES cadets(user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT CK_control_summaries_mark CHECK (final_mark BETWEEN 0 AND 10)
);

-- MEASUREMENT_UNITS

CREATE TABLE measurement_units (
    id       bigserial PRIMARY KEY,
    code     varchar(32) NOT NULL UNIQUE
);

-- STANDARDS

CREATE TABLE standards (
    id bigserial PRIMARY KEY,
    number smallint NOT NULL,
    name varchar(512) NOT NULL,
    measurement_unit_id bigint NOT NULL,
    course smallint NOT NULL,
    grade varchar(32) NOT NULL,
    time_value interval,
    int_value decimal(9,3),
    weight_category varchar(64),
    FOREIGN KEY (measurement_unit_id)
        REFERENCES measurement_units(id)
        ON UPDATE CASCADE
        ON DELETE NO ACTION,
    CONSTRAINT UQ_standard UNIQUE (number, course, grade, weight_category),
    CONSTRAINT CK_standards_course CHECK (course BETWEEN 1 AND 5),
    CONSTRAINT CK_standards_grade CHECK
        (grade IN ('Отлично', 'Хорошо', 'Удовлетворительно')),
    CONSTRAINT CK_standards_value CHECK
        (
            (time_value IS NOT NULL AND int_value IS NULL)
            OR
            (time_value IS NULL AND int_value IS NOT NULL)
        )
);

-- CONTROL_RESULTS

CREATE TABLE control_results (
    id          bigserial PRIMARY KEY,
    control_id  bigint NOT NULL,
    cadet_id    bigint NOT NULL,
    status      varchar(64) NOT NULL,
    standard_id bigint NOT NULL,
    mark        smallint,
    FOREIGN KEY (control_id) REFERENCES controls(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (cadet_id) REFERENCES cadets(user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (standard_id) REFERENCES standards(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (control_id, cadet_id, standard_id),
    CONSTRAINT CK_control_results_status CHECK (status IN
        ('Присутствует','Болен','Командировка','Гауптвахта')),
    CONSTRAINT CK_control_results_mark CHECK (mark BETWEEN 0 AND 5)
);

-- INSPECTORS

CREATE TABLE inspectors (
    id          bigserial PRIMARY KEY,
    control_id  bigint NOT NULL,
    teacher_id  bigint,
    first_name  varchar(64) NOT NULL,
    last_name   varchar(64) NOT NULL,
    patronymic  varchar(64),
    external    boolean NOT NULL DEFAULT false,
    FOREIGN KEY (control_id) REFERENCES controls(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
);

-- TRAININGS

CREATE TABLE trainings (
    id             bigserial PRIMARY KEY,
    cadet_id       bigint NOT NULL,
    date           date NOT NULL DEFAULT CURRENT_DATE,
    current_weight decimal(6,3),
    rest_period    smallint,
    type           varchar(64),
    FOREIGN KEY (cadet_id) REFERENCES cadets(user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT CK_trainings_weight CHECK (current_weight BETWEEN 30 AND 180),
    CONSTRAINT CK_trainings_rest CHECK (rest_period BETWEEN 0 AND 3600),
    CONSTRAINT CK_trainings_type CHECK (type IN ('Сила','Скорость','Выносливость'))
);

-- EXERCISE_CATALOGS

CREATE TABLE exercise_catalogs (
    id          bigserial PRIMARY KEY,
    code        varchar(64) NOT NULL UNIQUE,
    description text,
    type        varchar(64),
    CONSTRAINT CK_exercise_catalogs_type CHECK (type IN
        ('Сила','Скорость','Выносливость'))
);

-- EXERCISE_PARAMETERS

CREATE TABLE exercise_parameters (
    id                  bigserial PRIMARY KEY,
    exercise_catalog_id bigint NOT NULL,
    code                varchar(64) NOT NULL,
    measurement_unit_id bigint NOT NULL,
    default_time_value  interval,        
    default_int_value   decimal(9,3),    
    FOREIGN KEY (exercise_catalog_id)
        REFERENCES exercise_catalogs(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (measurement_unit_id)
        REFERENCES measurement_units(id)
        ON UPDATE CASCADE ON DELETE NO ACTION,
    UNIQUE (exercise_catalog_id, code),
    CONSTRAINT CK_exercise_parameters_value CHECK
        (
            (default_time_value IS NOT NULL AND default_int_value IS NULL)
            OR
            (default_time_value IS NULL AND default_int_value IS NOT NULL)
        )
);

-- EXERCISES_IN_TRAINING

CREATE TABLE exercises_in_training (
    id                  bigserial PRIMARY KEY,
    training_id         bigint NOT NULL,
    exercise_catalog_id bigint NOT NULL,
    rest_period         smallint,
    FOREIGN KEY (training_id)
        REFERENCES trainings(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (exercise_catalog_id)
        REFERENCES exercise_catalogs(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT CK_exercises_in_training_rest CHECK (rest_period BETWEEN 0 AND 3600)
);

-- APPROACHES

CREATE TABLE approaches (
    id                         bigserial PRIMARY KEY,
    exercise_in_training_id    bigint NOT NULL,
    approach_number            smallint NOT NULL,
    exercise_parameter_id      bigint NOT NULL,
    value                      decimal(9,3) NOT NULL,
    FOREIGN KEY (exercise_in_training_id)
        REFERENCES exercises_in_training(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (exercise_parameter_id)
        REFERENCES exercise_parameters(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (exercise_in_training_id, approach_number, exercise_parameter_id),
    CONSTRAINT CK_approaches_number CHECK (approach_number > 0),
    CONSTRAINT CK_approaches_value CHECK (value >= 0)
);

-- ARTICLES

CREATE TABLE articles (
    id                  bigserial PRIMARY KEY,
    topic               varchar(256) NOT NULL,
    publication_date    date NOT NULL DEFAULT CURRENT_DATE,
    text                text NOT NULL,
    CONSTRAINT CK_articles_publication_date CHECK (publication_date <= CURRENT_DATE)
);

-- TAGS

CREATE TABLE tags (
    id   bigserial PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE
);

-- ARTICLE_TAGS

CREATE TABLE article_tags (
    article_id bigint,
    tag_id     bigint,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE control_standards (
    control_id bigint NOT NULL,
    standard_number smallint NOT NULL,
    PRIMARY KEY (control_id, standard_number),
    FOREIGN KEY (control_id) REFERENCES controls(id) ON DELETE CASCADE
);



CREATE OR REPLACE FUNCTION update_university_yearly_average()
RETURNS TRIGGER AS $$
DECLARE
    uni_id bigint;
    control_year integer;
BEGIN
    -- Получаем ID университета и год контроля
    SELECT g.university_id, EXTRACT(YEAR FROM c.date) INTO uni_id, control_year
    FROM groups g
    JOIN cadets cd ON cd.group_id = g.id
    JOIN controls c ON c.id = NEW.control_id
    WHERE cd.user_id = NEW.cadet_id;

    -- Обновляем среднюю оценку университета за конкретный год
    UPDATE universities u
    SET mark = (
        SELECT COALESCE(AVG(cs.final_mark), 0)
        FROM control_summaries cs
        JOIN cadets c ON c.user_id = cs.cadet_id
        JOIN groups g ON g.id = c.group_id
        JOIN controls ct ON ct.id = cs.control_id
        WHERE g.university_id = uni_id
        AND EXTRACT(YEAR FROM ct.date) = control_year
        AND cs.final_mark IS NOT NULL
    )
    WHERE u.id = uni_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION update_university_average()
RETURNS TRIGGER AS $$
DECLARE
    uni_id bigint;
BEGIN
    -- Получаем ID университета через группу курсанта
    SELECT g.university_id INTO uni_id
    FROM groups g
    JOIN cadets c ON c.group_id = g.id
    WHERE c.user_id = NEW.cadet_id;

    -- Обновляем среднюю оценку университета
    UPDATE universities u
    SET mark = (
        SELECT COALESCE(AVG(cs.final_mark), 0)
        FROM control_summaries cs
        JOIN cadets c ON c.user_id = cs.cadet_id
        JOIN groups g ON g.id = c.group_id
        WHERE g.university_id = uni_id
        AND cs.final_mark IS NOT NULL
    )
    WHERE u.id = uni_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;




CREATE TRIGGER trigger_update_university_average
AFTER INSERT OR UPDATE ON control_summaries
FOR EACH ROW
EXECUTE FUNCTION update_university_average();

