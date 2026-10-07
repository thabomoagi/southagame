import { api } from '$lib/api';

function createAuth() {
	let user = $state<any>(null);

	function init() {
		if (typeof window === 'undefined') return;

		const saved = localStorage.getItem('user');

		if (saved) {
			try {
				user = JSON.parse(saved);
			} catch {
				localStorage.removeItem('user');
				user = null;
			}
		}
	}

	function setSession(data: any) {
		const accessToken = data?.accessToken;
		const refreshToken = data?.refreshToken;

		if (accessToken) {
			localStorage.setItem('accessToken', accessToken);
		}

		if (refreshToken) {
			localStorage.setItem('refreshToken', refreshToken);
		}

		if (data?.username || data?.userId) {
			user = {
				id: data.userId,
				userId: data.userId,
				username: data.username,
				email: data.email,
				profilePictureUrl: data.profilePictureUrl
			};

			localStorage.setItem('user', JSON.stringify(user));
		} else {
			user = null;
		}
	}

	return {
		get user() {
			return user;
		},

		init,

		async login(identifier: string, password: string) {
			const data = await api.login(identifier, password);
			setSession(data);
		},

		async register(
			username: string,
			email: string,
			password: string
		) {
			const data = await api.register(
				username,
				email,
				password
			);
			setSession(data);
		},

		updateUserState(patch: {
			username?: string;
			email?: string;
			profilePictureUrl?: string | null;
		}) {
			if (!user) return;

			user = {
				...user,
				...patch
			};

			localStorage.setItem(
				'user',
				JSON.stringify(user)
			);
		},

		logout() {
			localStorage.removeItem('accessToken');
			localStorage.removeItem('refreshToken');
			localStorage.removeItem('user');
			user = null;
		}
	};
}

export const auth = createAuth();