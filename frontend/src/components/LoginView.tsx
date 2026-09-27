import type { LoginViewProps } from './LoginViewProps'

/**
 * Login screen displayed before a session is available.
 *
 * @param props values and callbacks used by the form
 * @returns the login view markup
 */
export function LoginView(props: LoginViewProps) {
  const buttonLabel = getButtonLabel(props.isSubmitting)

  return (
    <section className="login-card">
      <p className="eyebrow">Davey Scaffolding</p>
      <h1>Log in</h1>
      <p className="login-copy">Sign in with your owner account to view jobs.</p>
      <form className="login-form" onSubmit={props.onSubmit}>
        <label htmlFor="login-email">
          <span>Email</span>
          <input
            autoComplete="email"
            id="login-email"
            name="email"
            onChange={(event) => props.onEmailChange(event.target.value)}
            required
            type="email"
            value={props.email}
          />
        </label>
        <label htmlFor="login-password">
          <span>Password</span>
          <input
            autoComplete="current-password"
            id="login-password"
            name="password"
            onChange={(event) => props.onPasswordChange(event.target.value)}
            required
            type="password"
            value={props.password}
          />
        </label>
        {renderLoginError(props.loginError)}
        <button disabled={props.isSubmitting} type="submit">
          {buttonLabel}
        </button>
      </form>
    </section>
  )
}

function getButtonLabel(isSubmitting: boolean) {
  if (isSubmitting) {
    return 'Logging in…'
  }

  return 'Log in'
}

function renderLoginError(loginError: string | null) {
  if (!loginError) {
    return null
  }

  return <p className="panel panel-error">{loginError}</p>
}
