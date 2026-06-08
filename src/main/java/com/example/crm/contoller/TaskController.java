package com.example.crm.contoller;

import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.repository.TaskRepository;
import com.example.crm.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public TaskRepository getTaskRepository() {
		return taskRepository;
	}

	public void setTaskRepository(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	private LocalDateTime createdAt = LocalDateTime.now(); // Add this and its getter/setter
    // Manager assigns a task to an executive
    @PostMapping("/manager/assign-task")
    public String assignTask(@RequestParam String title,
                             @RequestParam String description,
                             @RequestParam String dueDate,
                             @RequestParam String priority,
                             @RequestParam Long executiveId,
                             HttpSession session) {
        
        User manager = (User) session.getAttribute("user");
        User executive = userRepository.findById(executiveId).orElseThrow();

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(LocalDate.parse(dueDate));
        task.setPriority(priority);
        task.setExecutive(executive);
        task.setManager(manager);

        taskRepository.save(task);
        return "redirect:/manager/executives?success=task_assigned";
    }

    // Manager recalls (deletes) a task
    @GetMapping("/manager/delete-task/{id}")
    public String deleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        return "redirect:/manager/executives?success=task_deleted";
    }

    // Executive marks a task as finished
    @GetMapping("/executive/complete-task/{id}")
    public String completeTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setStatus("COMPLETED");
        taskRepository.save(task);
        return "redirect:/executive/tasks?success=task_done";
    }
    
    @PostMapping("/manager/tasks/complete")
    public String managerMarkTaskComplete(@RequestParam Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("COMPLETED");
        taskRepository.save(task);
        return "redirect:/manager/tasks?success=task_completed";
    }
    
    
}