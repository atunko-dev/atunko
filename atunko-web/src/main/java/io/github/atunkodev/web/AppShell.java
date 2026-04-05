package io.github.atunkodev.web;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

@Push
@StyleSheet("/lumo/lumo.css")
@StyleSheet("/lumo/utility.css")
public class AppShell implements AppShellConfigurator {}
