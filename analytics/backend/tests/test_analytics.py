import pytest
from fastapi.testclient import TestClient


def test_overview_counters(client: TestClient) -> None:
    response = client.get("/api/analytics/overview")

    assert response.status_code == 200
    assert response.json() == {
        "total_users": 2,
        "total_attempts": 3,
        "total_questions": 3,
        "total_categories": 1,
    }


def test_overview_counters_on_empty_database(empty_db_client: TestClient) -> None:
    response = empty_db_client.get("/api/analytics/overview")

    assert response.status_code == 200
    assert response.json() == {
        "total_users": 0,
        "total_attempts": 0,
        "total_questions": 0,
        "total_categories": 0,
    }


def test_most_played_questions_ranks_by_play_count(client: TestClient) -> None:
    response = client.get("/api/analytics/questions/most-played")

    assert response.status_code == 200
    assert [(row["id"], row["play_count"]) for row in response.json()] == [
        (1, 3),
        (2, 2),
        (3, 1),
    ]

    limited = client.get("/api/analytics/questions/most-played", params={"limit": 1})

    assert [row["id"] for row in limited.json()] == [1]


def test_most_missed_questions_counts_incorrect_answers(client: TestClient) -> None:
    response = client.get("/api/analytics/questions/most-missed")

    assert response.status_code == 200
    assert [
        (row["id"], row["miss_count"], row["total_attempts"])
        for row in response.json()
    ] == [(2, 2, 2), (1, 1, 3), (3, 0, 1)]


def test_hardest_and_easiest_questions_report_accuracy(client: TestClient) -> None:
    hardest = client.get("/api/analytics/questions/hardest").json()
    easiest = client.get("/api/analytics/questions/easiest").json()

    assert [
        (row["id"], row["total_attempts"], row["correct_count"], row["accuracy_percentage"])
        for row in hardest
    ] == [(2, 2, 0, 0.0), (1, 3, 2, 66.67), (3, 1, 1, 100.0)]

    assert [row["id"] for row in easiest] == [3, 1, 2]


def test_fastest_and_slowest_questions_average_response_times(client: TestClient) -> None:
    fastest = client.get("/api/analytics/questions/fastest").json()
    slowest = client.get("/api/analytics/questions/slowest").json()

    assert [(row["id"], row["avg_time_ms"]) for row in fastest] == [
        (1, 1000.0),
        (3, 2000.0),
        (2, 4000.0),
    ]

    assert [(row["id"], row["avg_time_ms"]) for row in slowest] == [
        (2, 4000.0),
        (3, 2000.0),
        (1, 1000.0),
    ]


@pytest.mark.parametrize("limit", [0, 51])
def test_limit_outside_allowed_range_returns_422(client: TestClient, limit: int) -> None:
    response = client.get("/api/analytics/questions/most-played", params={"limit": limit})

    assert response.status_code == 422
