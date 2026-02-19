import { cva, type VariantProps } from 'class-variance-authority';

export const dialogVariants = cva(
  'bg-background data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 grid max-w-[calc(100%-2rem)] gap-4 ring-border ring-1 rounded p-4 text-sm duration-100 sm:max-w-sm fixed top-1/2 left-1/2 z-50 w-full -translate-x-1/2 -translate-y-1/2 outline-none',
);
export type ZardDialogVariants = VariantProps<typeof dialogVariants>;
