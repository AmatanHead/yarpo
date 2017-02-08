/*
 * Creates a window with a label and a button packed vertically
 */

#include "widgets.h"

#include <assert.h>
#include <string.h>
#include <stdbool.h>

int main(int argc, char *argv[])
{
    struct Application *app = Application_New();
    const char *app_class_name = Object_GetClassName((struct Object*) app);
    assert(0 == strcmp(app_class_name, "Application"));

    struct Widget *main_window = Widget_New(NULL);
    const char *window_class_name = Object_GetClassName((struct Object*) main_window);
    assert(0 == strcmp(window_class_name, "Widget"));

    struct Layout *layout = (struct Layout*) VBoxLayout_New(main_window);
    const char *layout_class_name = Object_GetClassName((struct Object*) layout);
    assert(0 == strcmp(layout_class_name, "VBoxLayout"));

    Widget_SetLayout(main_window, layout);

    struct Label *label = Label_New(main_window);
    const char *label_class_name = Object_GetClassName((struct Object*) label);
    assert(0 == strcmp(label_class_name, "Label"));

    Label_SetText(label, "Hello, World!");
    Layout_AddWidget(layout, (struct Widget*) label);

    struct PushButton *button = PushButton_New(main_window);
    const char *button_class_name = Object_GetClassName((struct Object*) button);
    assert(0 == strcmp(button_class_name, "PushButton"));

    PushButton_SetText(button, "Not Clickable");
    Layout_AddWidget(layout, (struct Widget*) button);

    Widget_SetWindowTitle(main_window, "Test 1");
    Widget_SetSize(main_window, 400, 300);
    Widget_SetVisible(main_window, true);

    int status = Application_Exec(app);

    // Also deletes all children of main_window
    Object_Delete((struct Object*) main_window);

    Object_Delete((struct Object*) app);

    assert(0 == status);

    return status;
}
