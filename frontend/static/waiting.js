// Change this URL to your deployed API health endpoint before deploying.
const HEALTH_URL = 'http://localhost:8080/api/v1/health';

const POLL_INTERVAL_MS = 3000;
const WAKE_TIMEOUT_MS = 3 * 60 * 1000;
const GRID_SIZE = 20;
const GAME_TICK_MS = 130;

const canvas = document.querySelector('#snake-canvas');
const context = canvas.getContext('2d');
const scoreElement = document.querySelector('#score');
const introMessage = document.querySelector('#intro-message');
const statusMessage = document.querySelector('#status-message');
const continueButton = document.querySelector('#continue-button');
const retryButton = document.querySelector('#retry-button');

let snake;
let food;
let direction;
let nextDirection;
let score;
let pollInterval;
let wakeTimeout;
let pollInProgress = false;

function resetSnake() {
	snake = [
		{ x: 8, y: 10 },
		{ x: 7, y: 10 },
		{ x: 6, y: 10 }
	];
	direction = { x: 1, y: 0 };
	nextDirection = direction;
	score = 0;
	scoreElement.textContent = String(score);
	placeFood();
	drawSnake();
}

function placeFood() {
	do {
		food = {
			x: Math.floor(Math.random() * GRID_SIZE),
			y: Math.floor(Math.random() * GRID_SIZE)
		};
	} while (snake.some((part) => part.x === food.x && part.y === food.y));
}

function drawSnake() {
	const size = canvas.clientWidth;
	const cellSize = size / GRID_SIZE;
	const pixelRatio = window.devicePixelRatio || 1;

	canvas.width = size * pixelRatio;
	canvas.height = size * pixelRatio;
	context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
	context.fillStyle = '#eaf6ee';
	context.fillRect(0, 0, size, size);

	context.fillStyle = '#d84a4a';
	context.beginPath();
	context.arc(
		(food.x + 0.5) * cellSize,
		(food.y + 0.5) * cellSize,
		cellSize * 0.32,
		0,
		Math.PI * 2
	);
	context.fill();

	snake.forEach((part, index) => {
		context.fillStyle = index === 0 ? '#005e49' : '#009a76';
		context.beginPath();
		context.roundRect(
			part.x * cellSize + 2,
			part.y * cellSize + 2,
			cellSize - 4,
			cellSize - 4,
			cellSize * 0.24
		);
		context.fill();
	});
}

function stepSnake() {
	direction = nextDirection;
	const head = {
		x: snake[0].x + direction.x,
		y: snake[0].y + direction.y
	};

	const hitWall =
		head.x < 0 ||
		head.x >= GRID_SIZE ||
		head.y < 0 ||
		head.y >= GRID_SIZE;
	const hitSelf = snake.some((part) => part.x === head.x && part.y === head.y);

	if (hitWall || hitSelf) {
		resetSnake();
		return;
	}

	snake.unshift(head);

	if (head.x === food.x && head.y === food.y) {
		score += 1;
		scoreElement.textContent = String(score);
		placeFood();
	} else {
		snake.pop();
	}

	drawSnake();
}

function setDirection(name) {
	const directions = {
		up: { x: 0, y: -1 },
		down: { x: 0, y: 1 },
		left: { x: -1, y: 0 },
		right: { x: 1, y: 0 }
	};
	const requested = directions[name];

	if (!requested) return;
	if (requested.x === -direction.x && requested.y === -direction.y) return;

	nextDirection = requested;
}

function stopPolling() {
	window.clearInterval(pollInterval);
	window.clearTimeout(wakeTimeout);
}

function showReady() {
	stopPolling();
	introMessage.hidden = true;
	continueButton.hidden = false;
	statusMessage.hidden = true;
	retryButton.hidden = true;
}

async function checkHealth() {
	if (pollInProgress) return;
	pollInProgress = true;

	try {
		const response = await fetch(HEALTH_URL, { cache: 'no-store' });

		if (response.status === 200) {
			showReady();
		}
	} catch {
		// An unavailable server or blocked cross-origin check means keep waiting.
	} finally {
		pollInProgress = false;
	}
}

function startPolling() {
	stopPolling();
	pollInProgress = false;
	introMessage.hidden = false;
	continueButton.hidden = true;
	retryButton.hidden = true;
	statusMessage.hidden = false;
	statusMessage.textContent = 'Checking if your game is ready...';

	checkHealth();
	pollInterval = window.setInterval(checkHealth, POLL_INTERVAL_MS);
	wakeTimeout = window.setTimeout(() => {
		stopPolling();
		statusMessage.textContent = 'Enjoy a game of Snake while we prepare your game.';
		retryButton.hidden = false;
	}, WAKE_TIMEOUT_MS);
}

document.querySelectorAll('[data-direction]').forEach((button) => {
	button.addEventListener('click', () => {
		setDirection(button.dataset.direction);
	});
});

window.addEventListener('keydown', (event) => {
	const keys = {
		ArrowUp: 'up',
		ArrowDown: 'down',
		ArrowLeft: 'left',
		ArrowRight: 'right',
		w: 'up',
		s: 'down',
		a: 'left',
		d: 'right'
	};
	const directionName = keys[event.key];

	if (directionName) {
		event.preventDefault();
		setDirection(directionName);
	}
});

window.addEventListener('resize', drawSnake);
retryButton.addEventListener('click', startPolling);

resetSnake();
window.setInterval(stepSnake, GAME_TICK_MS);
startPolling();
