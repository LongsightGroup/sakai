export const i18nUrl = /getI18nProperties.*dashboard$/;

export const i18n = `
welcome=Welcome
worksite_setup=Worksite Setup
worksite_setup_tooltip=Go to the worksite setup screen
motd=Message of the Day
save=Save
edit=Edit Dashboard
cancel=Cancel Editing
edit_tooltip=Edit the dashboard layout. When you're happy, click 'Save'
save_tooltip=Save the changes you've made to the dashboard layout
cancel_tooltip=Cancel any changed you've made to the dashboard layout. They won't be saved.
hide_header=Hide header
show_header=Show header
hide_header_tooltip=Hide the course image, title and status block
show_header_tooltip=Show the course image, title and status block
change_this_image=Change course image
show_motd_tooltip=Show the message of the day
hide_motd_tooltip=Hide the message of the day
template_picker_title=Choose a dashboard layout
template_picker_instruction=Select a dashboard layout for your site. You'll be able to  customise your dashboard widgets further, but first choose a base layout to start with.
option1=Option 1
option2=Option 2
option3=Option 3
select=Select
layout=Layout
layout_tooltip=Launch the layout picker
`;

export const siteId = "xyz";
export const overview = "This is the intro course about sex: Sex 101 (snogging)";
export const programme = "Emotional and Sexual Health";

export const dashboardUrl = `/api/sites/${siteId}/dashboard`;
export const dashboardData = {
  widgets: [ "announcements", "forums", "grades", "tasks" ],
  layout: [ "announcements", "forums", "grades", "tasks" ],
  template: 1,
  programme,
  overview,
};