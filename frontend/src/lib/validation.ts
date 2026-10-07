// Mirrors the backend PasswordValidator rule:
// at least 8 chars, at least one letter and one number, allowed chars.
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/;

export function isStrongPassword(password: string): boolean {
	return PASSWORD_PATTERN.test(password);
}

export const PASSWORD_HINT =
	'Password must be at least 8 characters long and contain at least one letter and one number.';