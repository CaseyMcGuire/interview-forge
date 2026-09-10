import stylex from "@stylexjs/stylex";

const styles = stylex.create({
  errorAlert: {
    backgroundColor: 'rgb(254, 226, 226)',
    padding: '16px',
    color: 'rgb(153, 25, 25)',
    borderRadius: '6px'
  }
})

type Props = {
  isVisible: boolean,
  text: string
}

export default function ErrorBanner(props: Props) {
  if (!props.isVisible) {
    return null
  }
  return (
    <div sx={styles.errorAlert}>
      {props.isVisible}
    </div>
  )
}