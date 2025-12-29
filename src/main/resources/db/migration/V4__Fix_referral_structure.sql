-- Add columns to referral_stats_logs for tracking signup counts
ALTER TABLE referral_stats_logs ADD COLUMN today_signup_count INT DEFAULT 0 NOT NULL;
ALTER TABLE referral_stats_logs ADD COLUMN total_signup_count INT DEFAULT 0 NOT NULL;

-- Add comments for new columns
COMMENT ON COLUMN referral_stats_logs.today_signup_count IS '오늘 가입자 수';
COMMENT ON COLUMN referral_stats_logs.total_signup_count IS '총 가입자 수';

