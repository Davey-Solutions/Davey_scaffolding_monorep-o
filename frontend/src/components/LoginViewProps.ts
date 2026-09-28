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
