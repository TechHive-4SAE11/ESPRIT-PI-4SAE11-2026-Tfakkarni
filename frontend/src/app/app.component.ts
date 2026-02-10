import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
  inputValue = '';

  onPrimaryClick() {
    console.log('Primary button clicked!');
    alert('Primary button clicked!');
  }

  onSecondaryClick() {
    console.log('Secondary button clicked!');
    alert('Secondary button clicked!');
  }

  onDestructiveClick() {
    console.log('Destructive button clicked with .prevent modifier!');
    alert('Destructive button clicked! (preventDefault was called)');
  }

  onDebounceInput(event: Event) {
    const target = event.target as HTMLInputElement;
    this.inputValue = target.value;
    console.log('Debounced input:', this.inputValue);
  }
}
