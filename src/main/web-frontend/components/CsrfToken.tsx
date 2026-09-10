import CsrfUtils from "utils/CsrfUtils";

export default function CsrfToken() {
  return (
    <input type="hidden" name="_csrf" value={CsrfUtils.getToken()}/>
  );
}