CREATE TABLE user_profiles ( -- stores user provided data for tdee and dashboard
  user_id UUID PRIMARY KEY DEFAULT auth.uid(),
  CONSTRAINT fk_user_id -- enforces user_id to match auth.uid
    FOREIGN KEY (user_id)
    REFERENCES auth.users(id)
    ON DELETE CASCADE,
  name VARCHAR(20),
  birthday DATE,
  height NUMERIC(5,2),
  current_goal VARCHAR(4),
  activity_multiplier NUMERIC(3,2),
  timezone VARCHAR(20)
);

CREATE TABLE weight_logs ( -- stores user weigh-in history for chart
  weight_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- primary key auto increment
  user_id UUID NOT NULL DEFAULT auth.uid(),
  weight_value NUMERIC(5,2) NOT NULL,
  date_recorded DATE NOT NULL,
  CONSTRAINT fk_weight_user -- enforces weight logs to be user based
    FOREIGN KEY (user_id)
    REFERENCES auth.users(id)
    ON DELETE CASCADE,
  CONSTRAINT unique_user_date -- enforces 1 weigh-in per day
    UNIQUE (user_id, date_recorded)
);

CREATE TABLE target_macros ( -- stores user's current macro targets
  target_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- primary key auto increment
  user_id UUID NOT NULL DEFAULT auth.uid(),
  CONSTRAINT fk_target_user_id -- enforces target macros to be user based
    FOREIGN KEY (user_id)
    REFERENCES auth.users(id)
    ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  calories NUMERIC NOT NULL, -- macros will be all calculated with the tdee
  protein NUMERIC NOT NULL,
  carbs NUMERIC NOT NULL,
  fats NUMERIC NOT NULL
);

CREATE TABLE meals ( -- stores meal macros
  meal_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- primary key auto increment
  user_id UUID NOT NULL DEFAULT auth.uid(),
  CONSTRAINT fk_meal_user_id -- enforces meals to be user based
  FOREIGN KEY (user_id)
    REFERENCES auth.users(id)
    ON DELETE CASCADE,
  title VARCHAR(100) NOT NULL,
  type VARCHAR(9) NOT NULL CHECK (type IN ('breakfast', 'lunch', 'snack', 'dinner')),
  calories NUMERIC NOT NULL,
  protein NUMERIC NOT NULL,
  carbs NUMERIC NOT NULL,
  fats NUMERIC NOT NULL,
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
