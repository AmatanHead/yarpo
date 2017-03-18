//
// Created by Vladimir Goncharov on 02.02.17.
//

#ifndef PY_SIMPLE_QT_WIDGETS_H
#define PY_SIMPLE_QT_WIDGETS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>

struct Object;
struct Widget /* extends Object */;
struct PushButton /* extends Widget */;
struct Label /* extends Widget */;
struct Application /* extends Object */;
struct Layout /* extends Object */;
struct VBoxLayout /* extends Layout */;

typedef void (NoArgumentsCallback)(struct Object *sender);

const char *Object_GetClassName(struct Object *object);
void Object_Delete(struct Object *object);

struct Application *Application_New();
int Application_Exec(struct Application *app);

struct VBoxLayout *VBoxLayout_New(struct Widget *parent);
void Layout_AddWidget(struct Layout *layout, struct Widget *widget);

struct Widget *Widget_New(struct Widget *parent);

void Widget_SetVisible(struct Widget *widget, bool v);
void Widget_SetWindowTitle(struct Widget *widget, const char *title);
void Widget_SetLayout(struct Widget *widget, struct Layout *layout);
void Widget_SetSize(struct Widget *widget, int w, int h);

struct Label *Label_New(struct Widget *parent);
void Label_SetText(struct Label *label, const char *text);

struct PushButton *PushButton_New(struct Widget *parent);
void PushButton_SetText(struct PushButton *button, const char *text);
void PushButton_SetOnClicked(struct PushButton *button, NoArgumentsCallback *callback);

#ifdef __cplusplus
} //extern "C"
#endif

#endif //PY_SIMPLE_QT_WIDGETS_H
