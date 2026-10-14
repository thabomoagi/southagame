// Change this URL to your deployed API health endpoint before deploying.
const HEALTH_URL = 'http://localhost:8080/api/v1/health';

const POLL_INTERVAL_MS = 3000;
const WAKE_TIMEOUT_MS = 3 * 60 * 1000;
const GRID_SIZE = 6;
const CANDY_TYPES = 5;

const canvas = document.querySelector('#candy-canvas');
const context = canvas.getContext('2d');
const scoreElement = document.querySelector('#score');
const introMessage = document.querySelector('#intro-message');
const statusMessage = document.querySelector('#status-message');
const continueButton = document.querySelector('#continue-button');
const retryButton = document.querySelector('#retry-button');

let board = [];
let selectedCell = null;
let score = 0;
let isAnimating = false;
let cascadeStep = 1;

let pollInterval;
let wakeTimeout;
let pollInProgress = false;

const candyConfig = {
	1: { color: '#e54b4b', symbol: '★', bg: '#fde8e8' },
	2: { color: '#3182ce', symbol: '■', bg: '#ebf8ff' },
	3: { color: '#38a169', symbol: '●', bg: '#f0fff4' },
	4: { color: '#d69e2e', symbol: '▲', bg: '#fefcbf' },
	5: { color: '#805ad5', symbol: '♦', bg: '#f3e8ff' }
};

function createRandomCandy() {
	return Math.floor(Math.random() * CANDY_TYPES) + 1;
}

function findMatches(grid) {
	const matched = Array.from({ length: GRID_SIZE }, () => Array(GRID_SIZE).false || Array(GRID_SIZE).fill(false));
	let hasMatch = false;

	// Check horizontal
	for (let r = 0; r < GRID_SIZE; r++) {
		for (let c = 0; c < GRID_SIZE - 2; c++) {
			const val = grid[r][c];
			if (val !== 0 && val === grid[r][c + 1] && val === grid[r][c + 2]) {
				matched[r][c] = true;
				matched[r][c + 1] = true;
				matched[r][c + 2] = true;
				hasMatch = true;
			}
		}
	}

	// Check vertical
	for (let c = 0; c < GRID_SIZE; c++) {
		for (let r = 0; r < GRID_SIZE - 2; r++) {
			const val = grid[r][c];
			if (val !== 0 && val === grid[r + 1][c] && val === grid[r + 2][c]) {
				matched[r][c] = true;
				matched[r + 1][c] = true;
				matched[r + 2][c] = true;
				hasMatch = true;
			}
		}
	}

	return { matched, hasMatch };
}

function hasValidMoves(grid) {
	for (let r = 0; r < GRID_SIZE; r++) {
		for (let c = 0; c < GRID_SIZE; c++) {
			const neighbors = [];
			if (r > 0) neighbors.push({ r: r - 1, c });
			if (r < GRID_SIZE - 1) neighbors.push({ r: r + 1, c });
			if (c > 0) neighbors.push({ r, c: c - 1 });
			if (c < GRID_SIZE - 1) neighbors.push({ r, c: c + 1 });

			for (const n of neighbors) {
				// Test swap
				const temp = grid[r][c];
				grid[r][c] = grid[n.r][n.c];
				grid[n.r][n.c] = temp;

				const { hasMatch } = findMatches(grid);

				// Swap back
				grid[n.r][n.c] = grid[r][c];
				grid[r][c] = temp;

				if (hasMatch) return true;
			}
		}
	}
	return false;
}

function initBoard() {
	score = 0;
	scoreElement.textContent = String(score);
	selectedCell = null;
	cascadeStep = 1;

	do {
		board = Array.from({ length: GRID_SIZE }, () =>
			Array.from({ length: GRID_SIZE }, () => createRandomCandy())
		);
	} while (findMatches(board).hasMatch || !hasValidMoves(board));

	drawBoard();
}

function drawBoard() {
	const size = canvas.clientWidth;
	const cellSize = size / GRID_SIZE;
	const pixelRatio = window.devicePixelRatio || 1;

	canvas.width = size * pixelRatio;
	canvas.height = size * pixelRatio;
	context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);

	context.fillStyle = '#eaf6ee';
	context.fillRect(0, 0, size, size);

	for (let r = 0; r < GRID_SIZE; r++) {
		for (let c = 0; c < GRID_SIZE; c++) {
			const candyId = board[r][c];
			const cfg = candyConfig[candyId] || candyConfig[1];
			const x = c * cellSize;
			const y = r * cellSize;
			const padding = 6;

			// Cell background
			context.fillStyle = cfg.bg;
			context.beginPath();
			context.roundRect(x + padding, y + padding, cellSize - padding * 2, cellSize - padding * 2, 12);
			context.fill();

			// Selection highlight
			if (selectedCell && selectedCell.row === r && selectedCell.col === c) {
				context.strokeStyle = '#007a5e';
				context.lineWidth = 4;
				context.stroke();
			} else {
				context.strokeStyle = '#b6d9c7';
				context.lineWidth = 1;
				context.stroke();
			}

			// Candy circle
			const cx = x + cellSize / 2;
			const cy = y + cellSize / 2;
			const radius = (cellSize - padding * 4) / 2;

			context.fillStyle = cfg.color;
			context.beginPath();
			context.arc(cx, cy, radius, 0, Math.PI * 2);
			context.fill();

			// Symbol
			context.fillStyle = '#ffffff';
			context.font = `bold ${Math.floor(cellSize * 0.35)}px sans-serif`;
			context.textAlign = 'center';
			context.textBaseline = 'middle';
			context.fillText(cfg.symbol, cx, cy);
		}
	}
}

async function processTurn(r1, c1, r2, c2) {
	isAnimating = true;

	// Swap
	let temp = board[r1][c1];
	board[r1][c1] = board[r2][c2];
	board[r2][c2] = temp;
	drawBoard();

	await new Promise((r) => setTimeout(r, 200));

	let { matched, hasMatch } = findMatches(board);

	if (!hasMatch) {
		// Swap back if no match
		temp = board[r1][c1];
		board[r1][c1] = board[r2][c2];
		board[r2][c2] = temp;
		drawBoard();
		selectedCell = null;
		isAnimating = false;
		return;
	}

	// Cascade loop
	cascadeStep = 1;
	while (hasMatch) {
		let matchCount = 0;
		for (let r = 0; r < GRID_SIZE; r++) {
			for (let c = 0; c < GRID_SIZE; c++) {
				if (matched[r][c]) {
					matchCount++;
					board[r][c] = 0;
				}
			}
		}

		score += matchCount * 10 * cascadeStep;
		scoreElement.textContent = String(score);
		drawBoard();
		await new Promise((r) => setTimeout(r, 250));

		// Gravity (candies fall)
		for (let c = 0; c < GRID_SIZE; c++) {
			let writeRow = GRID_SIZE - 1;
			for (let r = GRID_SIZE - 1; r >= 0; r--) {
				if (board[r][c] !== 0) {
					board[writeRow][c] = board[r][c];
					if (writeRow !== r) {
						board[r][c] = 0;
					}
					writeRow--;
				}
			}
			// Fill empty slots from top
			for (let r = writeRow; r >= 0; r--) {
				board[r][c] = createRandomCandy();
			}
		}

		drawBoard();
		await new Promise((r) => setTimeout(r, 250));

		cascadeStep++;
		const check = findMatches(board);
		matched = check.matched;
		hasMatch = check.hasMatch;
	}

	// Check if board has valid moves; if not, reshuffle
	if (!hasValidMoves(board)) {
		do {
			for (let r = 0; r < GRID_SIZE; r++) {
				for (let c = 0; c < GRID_SIZE; c++) {
					board[r][c] = createRandomCandy();
				}
			}
		} while (findMatches(board).hasMatch || !hasValidMoves(board));
	}

	selectedCell = null;
	drawBoard();
	isAnimating = false;
}

canvas.addEventListener('pointerdown', (e) => {
	if (isAnimating) return;

	const rect = canvas.getBoundingClientRect();
	const x = e.clientX - rect.left;
	const y = e.clientY - rect.top;
	const size = canvas.clientWidth;
	const cellSize = size / GRID_SIZE;

	const c = Math.floor(x / cellSize);
	const r = Math.floor(y / cellSize);

	if (r < 0 || r >= GRID_SIZE || c < 0 || c >= GRID_SIZE) return;

	if (!selectedCell) {
		selectedCell = { row: r, col: c };
		drawBoard();
	} else {
		if (selectedCell.row === r && selectedCell.col === c) {
			// Deselect
			selectedCell = null;
			drawBoard();
		} else {
			const dr = Math.abs(selectedCell.row - r);
			const dc = Math.abs(selectedCell.col - c);
			const isAdjacent = (dr === 1 && dc === 0) || (dr === 0 && dc === 1);

			if (isAdjacent) {
				const r1 = selectedCell.row;
				const c1 = selectedCell.col;
				processTurn(r1, c1, r, c);
			} else {
				// Move selection to new candy
				selectedCell = { row: r, col: c };
				drawBoard();
			}
		}
	}
});

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
		statusMessage.textContent = 'Enjoy a game of Candy Match while we prepare your game.';
		retryButton.hidden = false;
	}, WAKE_TIMEOUT_MS);
}

window.addEventListener('resize', drawBoard);
retryButton.addEventListener('click', startPolling);

initBoard();
startPolling();
