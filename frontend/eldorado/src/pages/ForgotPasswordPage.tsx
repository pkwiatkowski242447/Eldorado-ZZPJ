import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import eldoLogo from "@/assets/eldorado.png";
import {Button} from "@/components/ui/button.tsx";
import {z} from "zod";
import {useToast} from "@/components/ui/use-toast.ts";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {api} from "@/api/api.ts";
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from "@/components/ui/form.tsx";
import {Input} from "@/components/ui/input.tsx";
import {useTranslation} from "react-i18next";
import handleApiError from "@/components/HandleApiError.ts";

function ForgotPasswordPage() {
    const {toast} = useToast()
    const {t} = useTranslation();

    const formSchema = z.object({
        email: z.string().min(1, {message: t("forgotPasswordPage.emptyEmail")})
            .email(t("forgotPasswordPage.wrongEmail")),
    })

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            email: "",
        },
    })

    function onSubmit(values: z.infer<typeof formSchema>) {
        // console.table(values)
        api.forgotPassword(values.email)
            .then(() => {
                toast({
                    title: t("forgotPasswordPage.popUp.resetPasswordOK.title"),
                    description: t("forgotPasswordPage.popUp.resetPasswordOK.text"),
                });
            })
            .catch((error) => {
                handleApiError(error);
            });
    }

    return (
        <div className="flex flex-col items-center justify-center">
            <img src={eldoLogo} alt="Eldorado" className="h-auto w-1/2 mb-8"/>
            <Card className="mx-auto max-w-2xl">
                <CardHeader>
                    <CardTitle>{t("forgotPasswordPage.title")}</CardTitle>
                    <CardDescription>{t("forgotPasswordPage.info")}</CardDescription>
                </CardHeader>
                <CardContent>
                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(onSubmit)}>
                            <div className="flex flex-col space-y-4">
                                <div>
                                    <FormField
                                        control={form.control}
                                        name="email"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel
                                                    className="text-black">{t("forgotPasswordPage.email")}</FormLabel>
                                                <FormControl>
                                                    <Input placeholder="mail@example.com" {...field}/>
                                                </FormControl>
                                                <FormMessage/>
                                            </FormItem>
                                        )}
                                    />
                                </div>
                                <Button type="submit">{t("forgotPasswordPage.submit")}</Button>
                            </div>
                        </form>
                    </Form>
                </CardContent>
            </Card>
        </div>
    );
}

export default ForgotPasswordPage;