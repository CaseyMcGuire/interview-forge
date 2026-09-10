import AuthFormContainer from "../components/AuthFormContainer";
import stylex from "@stylexjs/stylex";
import FormField from "../components/FormField";
import CsrfToken from "../components/CsrfToken";

const styles = stylex.create({
  submitButton: {
    backgroundColor: 'rgb(37, 99, 235)',
    padding: '8px 16px',
    borderRadius: '4px',
    borderStyle: 'none',
    color: 'white',
    cursor: 'pointer',
    fontSize: '16px',
    width: '100%'
  },
  loginHeader: {
    fontSize: '24px',
    fontWeight: '700',
    marginBottom: '16px'
  },
})

export default function RegisterPage() {
  return (
    <AuthFormContainer>
      <form action="/user" method="post">
        <CsrfToken />
        <div sx={styles.loginHeader}>
          {'Create Account'}
        </div>
        <div>
          <FormField formName={"email"} placeholder={"Enter your email"} type={"text"} />
          <FormField formName={"password"} placeholder={"Enter your password"} type={"password"} />
          <input type="submit" sx={styles.submitButton} value="Submit" />
        </div>
      </form>
    </AuthFormContainer>
  )
}