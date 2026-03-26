import { cva, type VariantProps } from 'class-variance-authority';

export const dialogVariants = cva(
  'bg-background data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 grid gap-4 ring-border ring-1 rounded p-4 text-sm duration-100 z-50 outline-none !w-full !max-w-none',
);
export type ZardDialogVariants = VariantProps<typeof dialogVariants>;
