package lookup

// AbilityGuns are agents' guns that kills report as type "Weapon" with a null name.
// valorant-api.com doesn't list them; the IDs come from match data (patches 13.06–13.10).
// Tour De Force had an empty ID in 13.06, which can't be keyed.
var AbilityGuns = map[string]Weapon{
	"856d9a7e-4b06-dc37-15dc-9d809c37cb90": {Name: "Headhunter", AbilitySlot: "ability1"},
	"39099fb5-4293-def4-1e09-2e9080ce7456": {Name: "Tour De Force", AbilitySlot: "ultimate"},
	"95336ae4-45d4-1032-cfaf-6bad01910607": {Name: "Overdrive", AbilitySlot: "ultimate"},
}
