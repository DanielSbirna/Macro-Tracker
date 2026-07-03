-- user_profiles table policies for select, insert, update and delete
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "select_own_user_profiles"
  ON user_profiles FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "insert_own_user_profiles"
  ON user_profiles FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "update_own_user_profiles"
  ON user_profiles FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "delete_own_user_profiles"
  ON user_profiles FOR DELETE
  USING (user_id = auth.uid());

-- weight_logs table policies for select, insert, update and delete
ALTER TABLE weight_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "select_own_weight_logs"
  ON weight_logs FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "insert_own_weight_logs"
  ON weight_logs FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "update_own_weight_logs"
  ON weight_logs FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "delete_own_weight_logs"
  ON weight_logs FOR DELETE
  USING (user_id = auth.uid());

-- target_macros table policies for select, insert, update and delete
ALTER TABLE target_macros ENABLE ROW LEVEL SECURITY;

CREATE POLICY "select_own_target_macros"
  ON target_macros FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "insert_own_target_macros"
  ON target_macros FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "update_own_target_macros"
  ON target_macros FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "delete_own_target_macros"
  ON target_macros FOR DELETE
  USING (user_id = auth.uid());

-- meals table policies for select, insert, update and delete
ALTER TABLE meals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "select_own_meals"
  ON meals FOR SELECT
  USING (user_id = auth.uid());

CREATE POLICY "insert_own_meals"
  ON meals FOR INSERT
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "update_own_meals"
  ON meals FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "delete_own_meals"
  ON meals FOR DELETE
  USING (user_id = auth.uid());
