type FormTextInputProps = {
  id: string,
  name: string,
  label: string,
  isPassword?: boolean
}

export default function FormTextInput(props: FormTextInputProps) {
  const type = (props.isPassword ?? false) ? "password" : "text";

  return (
    <div>
      <label htmlFor={props.id}>{props.label}</label>
      <input type={type} id={props.id} name={props.name} />
    </div>
  )
}