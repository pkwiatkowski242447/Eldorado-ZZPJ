import {Button} from "@/components/ui/button"
import {Input} from "@/components/ui/input"
import {z} from "zod";
import {zodResolver} from "@hookform/resolvers/zod"
import {useForm} from "react-hook-form"
import {Form, FormControl, FormField, FormItem, FormMessage,} from "@/components/ui/form"
import {FormLabel} from "react-bootstrap";
import {useTranslation} from "react-i18next";
import {Dispatch, SetStateAction, useState} from "react";
import {Loader2} from "lucide-react";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import {CreateParkingType, SectorStrategy} from "@/types/Parking.ts";
import {api} from "@/api/api.ts";
import handleApiError from "@/components/HandleApiError.ts";

type createParkingFormProps = {
    setDialogOpen: Dispatch<SetStateAction<boolean>>
}

function CreateParkingForm({setDialogOpen}:createParkingFormProps) {
    const {t} = useTranslation();
    const [isLoading, setIsLoading] = useState(false);
    const [isAlertDialogOpen, setAlertDialogOpen] = useState(false);
    const [newParking, setNewParking] = useState<CreateParkingType>({city:"", street:"", zipCode:"", strategy:SectorStrategy.LEAST_OCCUPIED})
    const formSchema = z.object({
        city: z.string().min(2, {message: "ZMIEN"})
            .max(50, {message: "ZMIEN"}).regex(RegExp("^([a-zA-Z\\u0080-\\u024F]+(?:. |-| |'))*[a-zA-Z\\u0080-\\u024F]*$"), {message: "ZMIEN"}),
        street: z.string().min(2, {message: "ZMIEN"})
            .max(60, {message: "ZMIEN"}).regex(RegExp("^[A-Za-z0-9.-]{5,50}$"), {message: "ZMIEN"}),
        zipCode: z.string().min(6, {message: "ZMIEN"})
            .max(6, {message: "ZMIEN"}).regex(RegExp("^\\d{2}-\\d{3}$"), {message: "ZMIEN"}),
        strategy: z.enum(["LEAST_OCCUPIED", "MOST_OCCUPIED", "LEAST_OCCUPIED_WEIGHTED"])
    })

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            city: "",
            street: "",
            zipCode: "",
            strategy: "LEAST_OCCUPIED"
        },
    })

    async function handleAlertDialog(){
        api.createParking(newParking)
            .then(() => {
                setAlertDialogOpen(false);
                setDialogOpen(false)})
            .catch(error => {
                setAlertDialogOpen(false);
                handleApiError(error);
            });
    }

    async function onSubmit(values: z.infer<typeof formSchema>) {
        setIsLoading(true);
        try {
            setNewParking({
                city: values.city,
                street:values.street,
                zipCode:values.zipCode,
                strategy:SectorStrategy[values.strategy]
            })
            setAlertDialogOpen(true);
        } catch (error) {
            console.log(error);
        } finally {
            setIsLoading(false);
        }
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                    control={form.control}
                    name="city"
                    render={({field}) => (
                        <FormItem>
                            <div className="grid gap-4">
                                <FormLabel className="text-left">City</FormLabel>
                                <FormControl>
                                    <Input placeholder="Warszawa" {...field} />
                                </FormControl>
                                <FormMessage/>
                            </div>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name="street"
                    render={({field}) => (
                        <FormItem>
                            <div className="grid gap-4">
                                <FormLabel className="text-left">Street</FormLabel>
                                <FormControl>
                                    <Input placeholder="Ziemniaczana" {...field} />
                                </FormControl>
                                <FormMessage/>
                            </div>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name="zipCode"
                    render={({field}) => (
                        <FormItem>
                            <div className="grid gap-4">
                                <FormLabel className="text-left">Zip code</FormLabel>
                                <FormControl>
                                    <Input placeholder="12-345" {...field} />
                                </FormControl>
                                <FormMessage/>
                            </div>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name="strategy"
                    render={({field}) => (
                        <FormItem>
                            <div className="grid gap-4">
                                <FormLabel className="text-left">Strategy</FormLabel>
                                <Select onValueChange={field.onChange} defaultValue={field.value}>
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select a sector determination strategy" />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        <SelectItem value="LEAST_OCCUPIED">Least occupied</SelectItem>
                                        <SelectItem value="MOST_OCCUPIED">Most occupied</SelectItem>
                                        <SelectItem value="LEAST_OCCUPIED_WEIGHTED">Least occupied weighted</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </FormItem>
                    )}
                />
                <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                        </>
                    ) : (
                        t("parkingManagementPage.createParking")
                    )}
                </Button>
            </form>
            <AlertDialog open={isAlertDialogOpen} onOpenChange={setAlertDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogTitle>{t("general.confirm")}</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to create a new parking?
                    </AlertDialogDescription>
                    <AlertDialogAction onClick={handleAlertDialog}>
                        {t("general.ok")}
                    </AlertDialogAction>
                    <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                </AlertDialogContent>
            </AlertDialog>
        </Form>
    )
}

export default CreateParkingForm