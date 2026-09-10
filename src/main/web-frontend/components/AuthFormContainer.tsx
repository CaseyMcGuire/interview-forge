import stylex from "@stylexjs/stylex"

const styles = stylex.create({
  loginFormContainer: {
    display: 'flex',
    justifyContent: 'center',
    fontFamily: 'ui-sans-serif, system-ui, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"'
  },
  loginForm: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px',
    flexDirection: 'column',
    boxShadow: 'rgba(0, 0, 0, 0) 0px 0px 0px 0px',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: 'rgb(229, 231, 235)',
    height: '320px',
    width: '448px'
  }
})

export default function AuthFormContainer(props: { children: React.ReactNode }) {
  return (<div sx={styles.loginFormContainer}>
      <div sx={styles.loginForm}>
        {props.children}
      </div>
    </div>
  )
}