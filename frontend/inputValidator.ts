/**
 * Front-end check for free-text fields. This only improves the user experience:
 * the back-end must always validate again, since anything in the browser can be bypassed.
 *
 * The original version stored the pattern as a string and called .test() on it.
 * Strings have no .test() method, so that code would not have compiled:
 *
 *   private static readonly INVALID_CHARACTERS_REGEX = ".*[<>/()*!+\\[\\]{}\\\\].*";
 *   return !this.INVALID_CHARACTERS_REGEX.test(input);
 *
 * This version uses a RegExp literal and the same allowlist as the Java back-end.
 */
export const MAX_TEXT_LENGTH = 256;

const ALLOWED_TEXT = new RegExp(`^[\\p{L}\\p{N} .,\\-_:]{0,${MAX_TEXT_LENGTH}}$`, "u");

export function isValidInput(input: string | null | undefined): boolean {
  return typeof input === "string" && ALLOWED_TEXT.test(input);
}
