// Waiting Room MCQ Game with 20 preloaded South African trivia questions
const DEFAULT_HEALTH_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
	? 'http://localhost:8080/api/v1/health'
	: 'https://southagame.onrender.com/api/v1/health';

const HEALTH_URL = localStorage.getItem('SOUTH_API_HEALTH_URL') || DEFAULT_HEALTH_URL;
const POLL_INTERVAL_MS = 3000;

const questionBank = [
	{
		category: "Music",
		question: "Who sings the hit song “Nkalakatha”?",
		options: [
			{ id: "o1", text: "Mandoza", isCorrect: true },
			{ id: "o2", text: "Zola", isCorrect: false },
			{ id: "o3", text: "TKZee", isCorrect: false },
			{ id: "o4", text: "Brown Dash", isCorrect: false }
		]
	},
	{
		category: "Music",
		question: "What is Tyla's surname?",
		options: [
			{ id: "o1", text: "Seethal", isCorrect: true },
			{ id: "o2", text: "Mokoena", isCorrect: false },
			{ id: "o3", text: "Dlamini", isCorrect: false },
			{ id: "o4", text: "Mahlangu", isCorrect: false }
		]
	},
	{
		category: "Music",
		question: "Who sang “Imali Eningi” with Intaba Yase Dubai and Riky Rick?",
		options: [
			{ id: "o1", text: "Big Zulu", isCorrect: true },
			{ id: "o2", text: "Cassper Nyovest", isCorrect: false },
			{ id: "o3", text: "Nasty C", isCorrect: false },
			{ id: "o4", text: "K.O", isCorrect: false }
		]
	},
	{
		category: "Music",
		question: "Which South African group is famous for the song “Shibobo”?",
		options: [
			{ id: "o1", text: "TKZee", isCorrect: true },
			{ id: "o2", text: "Mafikizolo", isCorrect: false },
			{ id: "o3", text: "Freshlyground", isCorrect: false },
			{ id: "o4", text: "Bongo Maffin", isCorrect: false }
		]
	},
	{
		category: "Music",
		question: "Which South African singer released the international hit “Water”?",
		options: [
			{ id: "o1", text: "Tyla", isCorrect: true },
			{ id: "o2", text: "Lira", isCorrect: false },
			{ id: "o3", text: "Zahara", isCorrect: false },
			{ id: "o4", text: "Nomcebo Zikode", isCorrect: false }
		]
	},
	{
		category: "Culture",
		question: "What does “Mzansi” commonly mean?",
		options: [
			{ id: "o1", text: "South Africa", isCorrect: true },
			{ id: "o2", text: "Johannesburg", isCorrect: false },
			{ id: "o3", text: "Africa", isCorrect: false },
			{ id: "o4", text: "Cape Town", isCorrect: false }
		]
	},
	{
		category: "Culture",
		question: "Which language is commonly associated with the greeting “Sawubona”?",
		options: [
			{ id: "o1", text: "isiZulu", isCorrect: true },
			{ id: "o2", text: "Afrikaans", isCorrect: false },
			{ id: "o3", text: "Sesotho", isCorrect: false },
			{ id: "o4", text: "Tshivenda", isCorrect: false }
		]
	},
	{
		category: "Culture",
		question: "What is the traditional South African meat-and-maize meal called?",
		options: [
			{ id: "o1", text: "Pap and vleis", isCorrect: true },
			{ id: "o2", text: "Sushi", isCorrect: false },
			{ id: "o3", text: "Paella", isCorrect: false },
			{ id: "o4", text: "Lasagne", isCorrect: false }
		]
	},
	{
		category: "Culture",
		question: "What is a South African braai?",
		options: [
			{ id: "o1", text: "A barbecue", isCorrect: true },
			{ id: "o2", text: "A wedding dance", isCorrect: false },
			{ id: "o3", text: "A traditional hat", isCorrect: false },
			{ id: "o4", text: "A type of bread", isCorrect: false }
		]
	},
	{
		category: "Brands",
		question: "“Finger lickin’ good” is the slogan of which fast-food brand?",
		options: [
			{ id: "o1", text: "KFC", isCorrect: true },
			{ id: "o2", text: "Nando’s", isCorrect: false },
			{ id: "o3", text: "Chicken Licken", isCorrect: false },
			{ id: "o4", text: "Steers", isCorrect: false }
		]
	},
	{
		category: "Politics",
		question: "Who was South Africa's president in 2010?",
		options: [
			{ id: "o1", text: "Jacob Zuma", isCorrect: true },
			{ id: "o2", text: "Thabo Mbeki", isCorrect: false },
			{ id: "o3", text: "Nelson Mandela", isCorrect: false },
			{ id: "o4", text: "Kgalema Motlanthe", isCorrect: false }
		]
	},
	{
		category: "Politics",
		question: "Who is South Africa's current president as of October 2026?",
		options: [
			{ id: "o1", text: "Cyril Ramaphosa", isCorrect: true },
			{ id: "o2", text: "Jacob Zuma", isCorrect: false },
			{ id: "o3", text: "Julius Malema", isCorrect: false },
			{ id: "o4", text: "Thabo Mbeki", isCorrect: false }
		]
	},
	{
		category: "Politics",
		question: "In which year did South Africa hold its first democratic national election?",
		options: [
			{ id: "o1", text: "1994", isCorrect: true },
			{ id: "o2", text: "1990", isCorrect: false },
			{ id: "o3", text: "1996", isCorrect: false },
			{ id: "o4", text: "2000", isCorrect: false }
		]
	},
	{
		category: "History",
		question: "Who became South Africa's first democratically elected president?",
		options: [
			{ id: "o1", text: "Nelson Mandela", isCorrect: true },
			{ id: "o2", text: "Oliver Tambo", isCorrect: false },
			{ id: "o3", text: "Desmond Tutu", isCorrect: false },
			{ id: "o4", text: "Walter Sisulu", isCorrect: false }
		]
	},
	{
		category: "History",
		question: "What date is celebrated as Freedom Day in South Africa?",
		options: [
			{ id: "o1", text: "27 April", isCorrect: true },
			{ id: "o2", text: "16 June", isCorrect: false },
			{ id: "o3", text: "9 August", isCorrect: false },
			{ id: "o4", text: "24 September", isCorrect: false }
		]
	},
	{
		category: "History",
		question: "Which South African township is associated with the 1976 student uprising?",
		options: [
			{ id: "o1", text: "Soweto", isCorrect: true },
			{ id: "o2", text: "Alexandra", isCorrect: false },
			{ id: "o3", text: "Khayelitsha", isCorrect: false },
			{ id: "o4", text: "Katlehong", isCorrect: false }
		]
	},
	{
		category: "Sport",
		question: "What is the nickname of South Africa's national rugby team?",
		options: [
			{ id: "o1", text: "Springboks", isCorrect: true },
			{ id: "o2", text: "Proteas", isCorrect: false },
			{ id: "o3", text: "Bafana Bafana", isCorrect: false },
			{ id: "o4", text: "The Blitzboks", isCorrect: false }
		]
	},
	{
		category: "Sport",
		question: "What is the nickname of South Africa's men's national football team?",
		options: [
			{ id: "o1", text: "Bafana Bafana", isCorrect: true },
			{ id: "o2", text: "Springboks", isCorrect: false },
			{ id: "o3", text: "Proteas", isCorrect: false },
			{ id: "o4", text: "Amabokoboko", isCorrect: false }
		]
	},
	{
		category: "News & Sport",
		question: "Which country hosted the 2010 FIFA World Cup?",
		options: [
			{ id: "o1", text: "South Africa", isCorrect: true },
			{ id: "o2", text: "Brazil", isCorrect: false },
			{ id: "o3", text: "Germany", isCorrect: false },
			{ id: "o4", text: "Morocco", isCorrect: false }
		]
	},
	{
		category: "Culture",
		question: "What is Heritage Day commonly nicknamed in South Africa?",
		options: [
			{ id: "o1", text: "National Braai Day", isCorrect: true },
			{ id: "o2", text: "National Dance Day", isCorrect: false },
			{ id: "o3", text: "Freedom Feast", isCorrect: false },
			{ id: "o4", text: "Unity Sunday", isCorrect: false }
		]
	}
];

// App State
let currentQuestions = [];
let currentIndex = 0;
let score = 0;
let timeLeft = 120;
let timerId = null;
let locked = false;

// DOM Elements
const startScreen = document.querySelector('#start-screen');
const playScreen = document.querySelector('#play-screen');
const gameOverScreen = document.querySelector('#game-over-screen');
const startBtn = document.querySelector('#start-btn');
const playAgainBtn = document.querySelector('#play-again-btn');
const timerDisplay = document.querySelector('#timer-display');
const questionProgress = document.querySelector('#question-progress');
const questionText = document.querySelector('#question-text');
const optionsContainer = document.querySelector('#options-container');
const finalScoreDisplay = document.querySelector('#final-score-display');
const feedbackText = document.querySelector('#feedback-text');
const statusBarText = document.querySelector('#status-text');
const playFullGameBtn = document.querySelector('#play-full-game-btn');
const centralPlayFullGameBtn = document.querySelector('#central-play-full-game-btn');

function startRound() {
	// Load questions in exact sequence (Q1 to Q20)
	currentQuestions = [...questionBank];
	currentIndex = 0;
	score = 0;
	locked = false;
	timeLeft = 120;

	startScreen.hidden = true;
	gameOverScreen.hidden = true;
	playScreen.hidden = false;

	loadQuestion();
	startGlobalTimer();
}

function startGlobalTimer() {
	if (timerId) clearInterval(timerId);
	updateTimerDisplay();

	timerId = setInterval(() => {
		timeLeft--;
		updateTimerDisplay();

		if (timeLeft <= 0) {
			if (timerId) clearInterval(timerId);
			endRound();
		}
	}, 1000);
}

function updateTimerDisplay() {
	timerDisplay.textContent = String(timeLeft);
	timerDisplay.className = `text-7xl font-black tabular-nums ${
		timeLeft <= 10
			? 'text-red-500 animate-pulse'
			: timeLeft <= 30
			  ? 'text-sa-yellow'
			  : 'text-springbok dark:text-springbok-bright'
	}`;
}

function loadQuestion() {
	if (currentIndex >= currentQuestions.length) {
		endRound();
		return;
	}

	const q = currentQuestions[currentIndex];
	questionText.textContent = q.question;
	questionProgress.textContent = `Question ${currentIndex + 1} of ${currentQuestions.length}`;

	// Randomly shuffle options so correct answers are not always first
	const shuffledOptions = [...q.options].sort(() => Math.random() - 0.5);

	optionsContainer.innerHTML = '';
	shuffledOptions.forEach((opt) => {
		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'w-full rounded-2xl border-2 border-slate-200 bg-white px-6 py-5 text-left text-xl font-bold text-slate-800 transition-all active:bg-sa-yellow disabled:opacity-60 dark:border-slate-700 dark:bg-dark-surface dark:text-white';
		btn.textContent = opt.text;
		btn.addEventListener('click', () => handleAnswer(opt.isCorrect));
		optionsContainer.appendChild(btn);
	});

	locked = false;
}

function handleAnswer(isCorrect) {
	if (locked) return;
	locked = true;

	if (isCorrect) {
		score++;
	}

	currentIndex++;
	if (currentIndex >= currentQuestions.length) {
		endRound();
	} else {
		loadQuestion();
	}
}

function endRound() {
	if (timerId) clearInterval(timerId);
	playScreen.hidden = true;
	gameOverScreen.hidden = false;

	finalScoreDisplay.textContent = `${score} / ${currentQuestions.length} Points`;

	const percentage = (score / currentQuestions.length) * 100;
	if (percentage >= 100) {
		feedbackText.textContent = 'Absolute legend! You know Southa inside out!';
	} else if (percentage >= 80) {
		feedbackText.textContent = 'Sharp! Almost got all of them!';
	} else if (percentage >= 60) {
		feedbackText.textContent = 'Not bad at all, boet!';
	} else if (percentage >= 40) {
		feedbackText.textContent = 'Getting there! Keep practicing.';
	} else {
		feedbackText.textContent = 'Better luck next time, boet!';
	}
}

// Navigation handler for Play Full Game buttons
function handlePlayFullGame(e) {
	e.preventDefault();
	window.location.href = '/home';
}

if (playFullGameBtn) {
	playFullGameBtn.addEventListener('click', handlePlayFullGame);
}
if (centralPlayFullGameBtn) {
	centralPlayFullGameBtn.addEventListener('click', handlePlayFullGame);
}

// Server Health Check Polling
async function checkServerHealth() {
	try {
		const res = await fetch(HEALTH_URL, { cache: 'no-store' });
		if (res.status === 200) {
			statusBarText.textContent = 'Game server is ready!';
			playFullGameBtn.hidden = false;
			if (centralPlayFullGameBtn) {
				centralPlayFullGameBtn.hidden = false;
			}
		}
	} catch {
		statusBarText.textContent = 'Play full game';
		// For testing / convenience so the user can always click Play Full Game:
		// Remove hidden attribute if desired or keep it controlled by health check.
		// Let's make sure it's accessible or visible when needed, or controlled by server status.
	}
}

// Event Listeners
startBtn.addEventListener('click', startRound);
playAgainBtn.addEventListener('click', startRound);

// Start polling backend health
checkServerHealth();
window.setInterval(checkServerHealth, POLL_INTERVAL_MS);
