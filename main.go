package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"

	"github.com/go-chi/chi/middleware"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/salaboy/dapr-human-tasks/api"
)

var (
	AppPort    = getEnv("APP_PORT", "8081")
	KoDataPath = getEnv("KO_DATA_PATH", "kodata")
	tasks      = []Task{}
)

type Task struct {
	Id       string `json:"id"`
	Name     string `json:"name"`
	Payload  string `json:"payload"`
	Assignee string `json:"assignee"`
	Status   string `json:"status"`
}

func main() {
	r := NewChiServer()

	log.Printf("Starting Human Task Service in Port: %s", AppPort)
	// Start the server; this is a blocking call
	err := http.ListenAndServe(":"+AppPort, r)
	if err != http.ErrServerClosed {
		log.Panic(err)
	}
}

// OpenAPI OpenAPIHandler returns a handler that serves the OpenAPI documentation.
func OpenAPI(r *chi.Mux) {
	fs := http.FileServer(http.Dir(KoDataPath + "/docs/"))
	r.Handle("/openapi/*", http.StripPrefix("/openapi/", fs))
}

// getEnv is a helper function to get environment variable or return a default value.
func getEnv(key, fallback string) string {
	value, exists := os.LookupEnv(key)
	if !exists {
		value = fallback
	}
	return value
}

// server implements api.ServerInterface interface.
type server struct{}

// NewServer creates a new api.ServerInterface server.
func NewServer() api.ServerInterface {
	return &server{}
}

// GetAllTasks gets all tasks.
func (s server) GetAllTasks(w http.ResponseWriter, r *http.Request) {
	respondWithJSON(w, http.StatusOK, tasks)
}

// Assign a Task
// (POST /tasks/{id}/assign)
func (s server) AssignTask(w http.ResponseWriter, r *http.Request, id string) {

}

// Complete a Task
// (POST /tasks/{id}/complete)
func (s server) CompleteTask(w http.ResponseWriter, r *http.Request, id string) {
	log.Printf("Completing Task with id: %s", id)
	for i, t := range tasks {
		if t.Id == id {
			t.Status = "COMPLETED"
			tasks[i] = t
		}
	}
}

// Start a Task
// (POST /tasks/{id}/start)
func (s server) StartTask(w http.ResponseWriter, r *http.Request, id string) {
	log.Printf("Starting Task with id: %s", id)
	for i, t := range tasks {
		if t.Id == id {
			log.Printf("Task with id: %s found", id)
			t.Status = "STARTED"
			tasks[i] = t
		}
	}
}

// CreateTask creates a new task.
func (s server) CreateTask(w http.ResponseWriter, r *http.Request) {
	var task Task
	err := json.NewDecoder(r.Body).Decode(&task)
	if err != nil {
		log.Printf("There was an error decoding the request body into the struct: %v", err)
	}

	//Depending on the payload add status

	task.Id = uuid.New().String()

	if task.Assignee == "" {
		task.Status = "CREATED"
	}

	tasks = append(tasks, task)

	respondWithJSON(w, http.StatusOK, task)
}

// NewChiServer creates a new chi.Mux server.
func NewChiServer() *chi.Mux {

	r := chi.NewRouter()

	r.Use(middleware.Logger)

	fs := http.FileServer(http.Dir(KoDataPath))

	server := NewServer()

	OpenAPI(r)

	r.Mount("/api/", api.Handler(server))
	r.Handle("/*", http.StripPrefix("/", fs))

	// Add handlers for readiness and liveness endpoints
	r.HandleFunc("/health/{endpoint:readiness|liveness}", func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(map[string]bool{"ok": true})
	})

	return r
}

func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
