cmake_minimum_required(VERSION 3.6)
project(py)


include_directories(../include)


add_executable(test1 ${SOURCE_FILES} test1.c ../include/widgets.h)

target_link_libraries(test1 ${PYTHON_LIBRARIES})
target_link_libraries(test1 ${Qt5Core_LIBRARIES} ${Qt5Widgets_LIBRARIES})

target_link_libraries(test1 widgets)


add_executable(test2 ${SOURCE_FILES} test2.c ../include/widgets.h)

target_link_libraries(test2 ${PYTHON_LIBRARIES})
target_link_libraries(test2 ${Qt5Core_LIBRARIES} ${Qt5Widgets_LIBRARIES})

target_link_libraries(test2 widgets)


add_custom_command(
        OUTPUT ${EXECUTABLE_OUTPUT_PATH}/test3
        COMMAND mkdir -p ${EXECUTABLE_OUTPUT_PATH}
        COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/test3.py ${EXECUTABLE_OUTPUT_PATH}/test3
        COMMAND chmod +x ${EXECUTABLE_OUTPUT_PATH}/test3
        MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/test3.py
)

add_custom_target(test3 ALL DEPENDS ${EXECUTABLE_OUTPUT_PATH}/test3)


add_custom_command(
        OUTPUT ${EXECUTABLE_OUTPUT_PATH}/test4
        COMMAND mkdir -p ${EXECUTABLE_OUTPUT_PATH}
        COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/test4.py ${EXECUTABLE_OUTPUT_PATH}/test4
        COMMAND chmod +x ${EXECUTABLE_OUTPUT_PATH}/test4
        MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/test4.py
)

add_custom_target(test4 ALL DEPENDS ${EXECUTABLE_OUTPUT_PATH}/test4)


add_custom_command(
        OUTPUT ${EXECUTABLE_OUTPUT_PATH}/test5
        COMMAND mkdir -p ${EXECUTABLE_OUTPUT_PATH}
        COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/test5.py ${EXECUTABLE_OUTPUT_PATH}/test5
        COMMAND chmod +x ${EXECUTABLE_OUTPUT_PATH}/test5
        MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/test5.py
)

add_custom_target(test5 ALL DEPENDS ${EXECUTABLE_OUTPUT_PATH}/test5)


add_custom_target(tests DEPENDS test1 test2 test3 test4 test5)
