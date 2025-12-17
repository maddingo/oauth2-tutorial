import { Navigate } from 'react-router-dom';

const LoginHandler = ({ authentication, handleLoginRequest }) => {
  if (authentication === null) {
    return (
      <div><div>Loading...</div></div>
    );
  } else if (authentication === false) {
    return (
      <div>
        <h1>Welcome!</h1>
        <button
          onClick={() => {
            handleLoginRequest();
          }}
        >
          Sign In
        </button>
      </div>
    )
  } else {
    return (
      <Navigate to="/callback" />
    );
  }
};

export default LoginHandler;
