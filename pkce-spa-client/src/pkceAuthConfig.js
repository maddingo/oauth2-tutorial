const pkceAuthConfig = {
  authority: 'http://idp:9000/',
  client_id: 'public-client',
  redirect_uri: 'http://localhost:3000/callback',
  response_type: 'code',
  scope: 'openid profile',
  post_logout_redirect_uri: 'http://localhost:3000/',
  userinfo_endpoint: 'http://idp:9000/userinfo',
  response_mode: 'query',
  code_challenge_method: 'S256',
};

export default pkceAuthConfig;
