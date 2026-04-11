---
description: How to create and manage Skills in the Tfakkarni project
---

# Skills Management Workflow

Skills are specialized sets of instructions, scripts, and resources that extend the AI's capabilities for specific tasks within the Tfakkarni project.

## How to Create a New Skill

1. Identify a repetitive or complex task (e.g., adding a new microservice, creating a Zard UI component).
2. Create a folder in `_agent/skills/<skill-name>`.
3. Create a `SKILL.md` file inside that folder.
4. Define the skill's name and description in the YAML frontmatter of `SKILL.md`.
5. Add detailed instructions and patterns in the markdown content.
6. (Optional) Add a `scripts/` directory for helper scripts or a `templates/` directory for boilerplate code.

## How to Use a Skill

1. When starting a task, check if a relevant skill exists in `_agent/skills/`.
2. Use the `view_file` tool to read the `SKILL.md` file of the relevant skill.
3. Follow the instructions and use the provided scripts/templates to complete your task.

## Existing Skills
- (List your skills here as you create them)

## Integration with MCP Tools
Skills can leverage the project's MCP tools for enhanced performance:
- **Memory MCP**: Store skill-related patterns or common fixes as "nodes" in the knowledge graph.
- **GitHub MCP**: Automate skill-related repository actions (e.g., boilerplate branch creation).
- **Brave Search MCP**: Regularly update skill documentation by searching for the latest upstream changes in external dependencies.

## Best Practices
- Keep skill instructions concise and actionable.
- Link to project-specific documentation (e.g., `readme.md`, `COMPLETE_CRUD_GUIDE.md`).
- Version control your skills alongside the code.
