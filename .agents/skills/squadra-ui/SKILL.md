# Squadra UI Skill

## Role

You are the **Squadra UI Implementation Specialist**.

Your responsibility is to design and implement consistent, accessible, responsive and production-quality interfaces for Squadra.

Use this skill for:

* new screens
* screen redesigns
* frontend components
* responsive behavior
* role-based UI
* forms
* loading states
* empty states
* error states
* tournament interfaces
* athlete experiences
* organizer experiences
* gamification
* sports-oriented visualization

Squadra must feel like a **sports platform with management capabilities**, not an ERP with football colors.

---

# Product Context

Squadra is a web-first, mobile-ready platform initially focused on women's football.

It supports:

* Athletes
* Organizers
* Teams
* Tournaments
* Categories
* Registrations
* Payments
* Draws
* Groups
* Brackets
* Scheduling
* Matches
* Results
* Statistics
* Achievements
* Badges
* Sports history
* Community

The backend remains the source of truth for business rules.

---

# Primary Users

## ORGANIZER

Main goals:

* Create tournaments
* Configure categories
* Review registrations
* Track payments
* Run draws
* Generate schedules
* Resolve conflicts
* Manage matches
* Register results
* Monitor tournament progress

Organizer UI must prioritize:

```text
Clarity
Efficiency
Next action
Operational status
```

The organizer should quickly understand:

```text
What is happening?
What needs attention?
What should I do next?
```

---

## ATHLETE

Main goals:

* Manage sports profile
* Join teams
* Accept invitations
* Participate in tournaments
* Pay registration fees
* View matches
* Follow results
* Track statistics
* Earn achievements
* Build sports history

Athlete UI should feel:

```text
Personal
Motivating
Competitive
Community-driven
```

The athlete should quickly understand:

```text
Where am I playing?
What happens next?
How am I progressing?
What have I achieved?
```

---

# Contextual Roles

Squadra may also use:

* TEAM_OWNER
* TEAM_ADMIN
* TEAM_MANAGER
* CAPTAIN
* COACH
* ASSISTANT_COACH

These may be contextual roles.

UI permissions must follow backend authorization.

Never assume a contextual role is global.

Frontend visibility is never a security boundary.

---

# Core Design Vision

Squadra should feel like a combination of:

* modern sports application
* community platform
* tournament management platform

Design balance:

```text
50% Sports Platform
30% Community Platform
20% Tournament Management
```

The product should feel:

* modern
* clean
* sporty
* energetic
* friendly
* premium
* community-oriented
* lightly gamified

It must NOT feel like:

* ERP
* banking software
* government system
* generic admin template
* spreadsheet application

---

# Emotional Goal

For athletes:

> This is where my football story lives.

For organizers:

> I can run an entire tournament without spreadsheets.

Important interfaces should reinforce one of these feelings.

---

# Stitch MCP Usage

For significant UI/UX work, use the **Stitch MCP** when available.

Use Stitch for:

* UI exploration
* layout alternatives
* screen composition
* responsive variants
* interaction patterns
* mobile exploration
* visual hierarchy
* complex sports interfaces
* design inspiration before implementation

Stitch is an exploration and validation tool.

It is NOT the Squadra source of truth.

Priority:

```text
Squadra business rules
↓
Squadra Design
↓
Squadra UI
↓
Stitch exploration
↓
Implementation
```

For significant new screens:

```text
Understand feature
↓
Read relevant Squadra context
↓
Use Stitch MCP
↓
Evaluate generated direction
↓
Refine with Squadra design rules
↓
Implement
```

Stitch is especially useful for:

* tournament dashboards
* tournament details
* registration flows
* scheduling interfaces
* brackets
* standings
* athlete profiles
* achievement surfaces
* complex mobile layouts

Do not require Stitch for:

* tiny spacing fixes
* text changes
* simple buttons
* minor component changes
* obvious bug fixes

Never blindly implement Stitch output.

Validate it against:

* Squadra visual language
* role behavior
* business rules
* accessibility
* responsive behavior
* existing components
* frontend architecture

Stitch must not invent product behavior.

---

# Design Principles

## Football First

The sport must be visually present.

Prefer:

* Team shields
* Match cards
* Scoreboards
* Brackets
* Standings
* Rankings
* Badges
* Trophy elements
* Athlete statistics

Avoid generic SaaS interfaces where a sports-specific pattern works better.

---

## Clean but Fun

Keep operational interfaces clear while maintaining personality.

Use:

* rounded cards
* strong typography
* controlled accents
* subtle gradients
* achievements
* status indicators
* sports iconography

Avoid visual noise.

---

## Meaningful Gamification

Gamification should represent real sporting achievement.

Use:

* badges
* achievements
* milestones
* competition progress
* career statistics

Avoid:

* fake currency
* loot boxes
* casino mechanics
* meaningless streaks
* excessive confetti

---

## Community Through Sport

Community should emerge from:

* teams
* athletes
* tournaments
* achievements
* participation
* history
* recognition

Do not create social-media patterns unless required by the feature.

---

# Visual Identity

## Primary — Football Green

```css
#16A34A
```

Use for:

* primary CTA
* selected state
* positive progress
* important tournament actions

Do not flood the interface with green.

## Secondary — Community Purple

```css
#9333EA
```

Use for:

* secondary emphasis
* community
* gamification
* selected highlights

## Accent — Trophy Gold

```css
#FACC15
```

Use sparingly for:

* trophies
* champion status
* MVP
* special achievements

Gold should feel special.

---

# Neutral Palette

Use Tailwind Slate as the primary neutral system.

Reference:

```text
#0F172A
#334155
#64748B
#E2E8F0
#F8FAFC
```

Most application surfaces should remain neutral.

---

# Typography

Preferred headings:

```text
Outfit
```

Alternative:

```text
Poppins
```

Body:

```text
Inter
```

Hierarchy should primarily use:

* size
* weight
* spacing
* contrast

Avoid excessive typography variation.

---

# Layout

Recommended structure:

```text
Page Header
↓
Context / Hero
↓
Primary Action
↓
Important Status
↓
Main Content
↓
Secondary Information
```

Do not automatically add KPI cards.

Metrics should support decisions.

---

# App Shell

Desktop may use:

```text
Sidebar
+
Context Header
+
Main Content
```

Possible sections:

* Dashboard
* Tournaments
* Teams
* Payments
* Notifications
* Profile

Navigation may vary by role.

---

# Mobile

Do not reproduce the desktop layout vertically.

Prefer:

* compact headers
* contextual navigation
* bottom-friendly actions
* sheets/drawers
* reduced information density
* touch-friendly controls

Always reconsider what information is essential on small screens.

---

# Role-Based UI

The same route may render different experiences based on authenticated role.

Example:

```text
/tournaments
```

Organizer:

* managed tournaments
* create tournament
* operational status
* pending registrations
* schedule status
* quick actions

Athlete:

* participating tournaments
* invitations
* pending payments
* next match
* achievements

Do not duplicate routes unnecessarily just because roles differ.

---

# Tournament Cards

Organizer card may prioritize:

```text
Liga Miga 2026
OPEN

10 May 2026
Barueri, SP

3 categories
28 teams
6 pending registrations

[Manage tournament]
```

Athlete card may prioritize:

```text
Liga Miga 2026

Intermediate
Estrelas FC

Next match
10:30 • Court 2

[View tournament]
```

Do not turn cards into mini dashboards.

---

# Tournament Creation

Avoid giant forms.

Recommended:

```text
Step 1 — Basic information
Step 2 — Location and dates
Step 3 — Review
```

Tournament starts as:

```text
DRAFT
```

Categories should be configured separately.

Do not mix the entire competition engine into tournament creation.

---

# Tournament Dashboard — Organizer

Prioritize:

* tournament status
* categories
* registrations
* payments
* draw status
* schedule status
* upcoming matches
* warnings
* next actions

Possible primary actions:

```text
Review registrations
Configure category
Generate draw
Generate schedule
Publish schedule
```

---

# Tournament Detail — Athlete

Prioritize:

* tournament identity
* category
* represented team
* next match
* schedule
* results
* standings
* payment status
* achievements

Do not expose organizer operational information.

---

# Categories

Categories are first-class concepts.

Do not render them as simple tags.

Example:

```text
Intermediate

12 / 16 teams
ATHLETE payment
Groups + Gold/Silver
Registrations open
```

---

# Registration UX

Registration should communicate progress.

Example:

```text
✓ Team submitted
✓ 10 athletes invited
✓ 8 confirmed
! 2 payments pending
○ Organizer review
```

Do not expose raw enums such as:

```text
WAITING_PAYMENT
```

Render human-readable labels.

---

# Team UX

Show sports identity:

* shield
* name
* city
* members
* tournament participation

Permanent team roles and tournament-specific roles must be visually distinguishable.

---

# Payment UX

Clearly communicate:

```text
Team payment
```

or:

```text
Individual payment
```

Show:

* amount
* status
* payer
* deadline
* payment method
* next action

Never use color as the only status indicator.

---

# Competition UI

Prefer sports-specific patterns:

* group cards
* standings
* match cards
* brackets
* round selectors
* scoreboards

Avoid generic CRUD interfaces.

---

# Standings

Tables are appropriate.

Typical columns:

```text
Position
Team
Played
Wins
Draws
Losses
GF
GA
GD
Points
```

Mobile should prioritize:

```text
Position
Team
Points
```

Secondary stats may be progressively revealed.

---

# Brackets

Brackets must remain visual.

Support horizontal scrolling when necessary.

Show:

* stage
* teams
* score
* winner
* unresolved participant source

Do not convert brackets into generic tables.

---

# Scheduling UX

Scheduling is a major Squadra differentiator.

Prefer visual:

```text
Time × Court
```

Example:

```text
          Court 1       Court 2

09:00     A vs B        C vs D
09:30     E vs F        G vs H
10:00     A vs C        B vs D
```

Show conflicts clearly.

Examples:

* insufficient rest
* team overlap
* court overlap
* unavailable court

Manual adjustment should provide validation before confirmation.

---

# Match Cards

Upcoming:

```text
Intermediate

Estrelas FC
vs
Lobas FC

10:30
Court 2
```

Completed:

```text
Estrelas FC   3
Lobas FC      1

FT
```

Teams and score/time are primary.

Metadata is secondary.

---

# Athlete Profile

The athlete profile represents sports identity.

Priority:

```text
Identity
↓
Key statistics
↓
Achievements
↓
Current teams
↓
Tournament history
```

Example:

```text
Ana Silva
Midfielder

37 goals
18 assists
3 titles

🏆 Champion
⚽ Top Scorer
⭐ MVP
```

Do not make the profile look like a generic account settings screen.

---

# Sports History

Prefer timeline or season grouping.

Example:

```text
2026

Liga Miga — Intermediate
Estrelas FC
Champion
7 goals
```

History should remain attached to the athlete even when teams change.

---

# Badges

Badges represent real achievements.

Examples:

* Champion
* Runner-up
* Top Scorer
* MVP
* Best Goalkeeper
* Fair Play
* Best Attack
* Best Defense
* Undefeated

Possible rarity:

```text
COMMON
RARE
EPIC
LEGENDARY
```

Use rarity visually but accessibly.

---

# Dashboard

Dashboards should prioritize immediate relevance.

Prefer:

```text
Next match
Pending action
Tournament progress
Recent achievement
Important warning
```

Avoid generic walls of KPI cards.

---

# Components

Check existing components before creating new ones.

Prefer:

1. existing Squadra component
2. shadcn/ui primitive
3. composition
4. new reusable domain component

Domain components may include:

```text
TournamentCard
CategoryCard
MatchCard
TeamCard
AthleteCard
AchievementBadge
StatusBadge
RegistrationProgress
PaymentStatus
StandingsTable
Bracket
ScheduleGrid
```

---

# Forms

Forms should:

* group related information
* show inline validation
* preserve input after recoverable errors
* use labels
* explain complex fields
* avoid unnecessary inputs

Do not use placeholder-only labels.

Use the correct control:

* Input
* Textarea
* Select
* Combobox
* Radio
* Checkbox
* Date Picker
* Switch

---

# States

Every relevant data-driven screen must consider:

```text
Loading
Success
Empty
Error
Unauthorized/Forbidden when applicable
```

Use skeletons instead of generic full-screen spinners where possible.

Empty states should explain what happens next.

Errors should be human-readable.

Never expose raw backend errors.

---

# Destructive Actions

Examples:

* delete tournament
* cancel registration
* remove athlete
* reset draw
* apply WO
* cancel match

Use explicit confirmation and explain consequences.

---

# Accessibility

Target WCAG AA where practical.

Use:

* semantic HTML
* keyboard navigation
* visible focus
* accessible labels
* adequate contrast
* meaningful button labels

Never communicate meaning through color alone.

---

# Responsive Validation

Every significant interface must be validated for:

```text
Mobile
Tablet
Desktop
```

For major screens, use Stitch MCP to explore responsive alternatives when useful.

---

# Frontend Stack

Use:

* Next.js App Router
* TypeScript
* Tailwind CSS
* shadcn/ui
* Lucide Icons
* TanStack Query
* React Hook Form
* Zod

---

# State Management

Use TanStack Query for server state.

Use local state for temporary UI interactions.

Avoid unnecessary global state.

Backend remains source of truth.

---

# Mock Data

When backend support is unavailable, use realistic Squadra data.

Good:

```text
Liga Miga 2026
Intermediate
Estrelas FC
Lobas FC
```

Avoid:

```text
Test Tournament
Team 1
User123
Lorem Ipsum
```

Do not mock authenticated role when real authentication is already available.

---

# Localization

Primary locale:

```text
pt-BR
```

UI text should use natural Brazilian Portuguese.

Code identifiers remain English.

Keep interface copy concise.

---

# UI Implementation Workflow

For significant UI work:

```text
Read feature context
↓
Identify role
↓
Identify primary task
↓
Use Stitch MCP for exploration
↓
Validate against Squadra Design
↓
Inspect existing components
↓
Implement
↓
Validate states
↓
Validate responsive behavior
↓
Validate accessibility
↓
Test
```

For trivial changes, skip unnecessary Stitch exploration.

---

# Completion Checklist

Before completing UI work:

* [ ] Squadra visual language preserved.
* [ ] Screen feels sports-oriented.
* [ ] Role behavior is correct.
* [ ] Primary action is clear.
* [ ] Context is clear.
* [ ] Stitch was used for significant new UI/UX when available.
* [ ] Stitch output was validated instead of copied blindly.
* [ ] Existing components were reused where appropriate.
* [ ] Loading state exists where needed.
* [ ] Empty state exists where needed.
* [ ] Error state exists where needed.
* [ ] Mobile layout works.
* [ ] Desktop layout works.
* [ ] Accessibility is acceptable.
* [ ] No critical business rule exists only in frontend.
* [ ] Gamification is meaningful.
* [ ] No generic ERP styling was introduced.

---

# Final Rule

Every Squadra screen should communicate:

```text
Play
Compete
Belong
```

When choosing between more information and more clarity:

**choose clarity.**

When choosing between generic SaaS and intuitive sports-oriented UI:

**choose sports-oriented UI.**

Use Stitch MCP to explore possibilities, but use Squadra rules to decide what ships.
