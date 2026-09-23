module.exports = {
  apps: [
    {
      name: "marketplace-scraper",
      cwd: __dirname,
      script: "./run_api.sh",
      interpreter: "bash",
      autorestart: true,
      watch: false,
      restart_delay: 5000,
      kill_timeout: 30000,
      merge_logs: true,
      time: false,
      env: {
        APP_ENV: "production",
      },
    },
  ],
};
