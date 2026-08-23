import type { FormEventHandler } from 'react'

/**
 * Props required to render the login form.
 */
export interface LoginViewProps {
  /** Current email field value. */
  email: string
  /** Current password field value. */
  password: string
  /** Whether the form submit is in progress. */
  isSubmitting: boolean
  /** Optional error shown above the submit button. */
  loginError: string | null
  /** Called when the email input changes. */
  onEmailChange: (value: string) => void
  /** Called when the password input changes. */
  onPasswordChange: (value: string) => void
  /** Called when the form is submitted. */
  onSubmit: FormEventHandler<HTMLFormElement>
}

/**
 * Login screen displayed before a session is available.
 *
 * @param props values and callbacks used by the form
 * @returns the login view markup
 */
export function LoginView(props: LoginViewProps) {
  const buttonLabel = props.isSubmitting ? 'Logging in…' : 'Log in'

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
        {props.loginError ? <p className="panel panel-error">{props.loginError}</p> : null}
        <button disabled={props.isSubmitting} type="submit">
          {buttonLabel}
        </button>
      </form>
    </section>
  )
}
